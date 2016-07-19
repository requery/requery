/*
 * Copyright 2016 requery.io
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.requery.processor;

import io.requery.Embedded;
import io.requery.Entity;
import io.requery.Superclass;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.requery.processor.EntityProcessor.GENERATE_ALWAYS;
import static io.requery.processor.EntityProcessor.GENERATE_JPA;
import static io.requery.processor.EntityProcessor.GENERATE_MODEL;

/**
 * {@link javax.annotation.processing.Processor} for generating entities based on annotations.
 *
 * @author Nikhil Purushe
 */
@SupportedAnnotationTypes({"io.requery.*", "javax.persistence.*"})
@SupportedOptions({GENERATE_MODEL, GENERATE_ALWAYS, GENERATE_JPA})
public final class EntityProcessor extends AbstractProcessor {

    static final String GENERATE_MODEL = "generate.model";
    static final String GENERATE_ALWAYS = "generate.always";
    static final String GENERATE_JPA = "generate.jpa";

    private Map<String, EntityGraph> graphs;
    private Map<TypeElement, EntityType> superTypes;
    private Map<TypeElement, EntityType> embeddedTypes;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        graphs = new LinkedHashMap<>();
        superTypes = new LinkedHashMap<>();
        embeddedTypes = new LinkedHashMap<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // types to generate in this round
        Map<TypeElement, EntityType> entities = new HashMap<>();
        SourceLanguage.map(processingEnv);
        Types types = processingEnv.getTypeUtils();

        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                typeElementOf(element).ifPresent(typeElement -> {
                    EntityType entity = null;
                    if (isEntity(typeElement)) {
                        // create or get the entity for the annotation
                        entity = computeType(entities, typeElement);
                        // create or get the graph for it
                        String model = entity.modelName();
                        graphs.computeIfAbsent(model,
                                key -> new EntityGraph(types, embeddedTypes)).add(entity);
                    } else if (isSuperclass(typeElement)) {
                        entity = computeType(superTypes, typeElement);
                    } else if (isEmbeddable(typeElement)) {
                        entity = computeType(embeddedTypes, typeElement);
                    }
                    if (entity != null) {
                        entity.addAnnotationElement(annotation, element);
                    }
                });
            }
        }
        // process
        boolean hasErrors = false;
        Set<ElementValidator> validators = new LinkedHashSet<>();
        Elements elements = processingEnv.getElementUtils();

        for (EntityType entity : entities.values()) {
            // add the annotated elements from the super type (if any)
            if (entity.element().getKind() == ElementKind.INTERFACE) {
                List<? extends TypeMirror> interfaces = entity.element().getInterfaces();
                for (TypeMirror mirror : interfaces) {
                    TypeElement superElement = elements.getTypeElement(mirror.toString());
                    if (superElement != null) {
                        mergeSuperType(entity, superElement);
                    }
                }
            }
            TypeMirror typeMirror = entity.element().getSuperclass();
            while (typeMirror.getKind() != TypeKind.NONE) {
                TypeElement superElement = elements.getTypeElement(typeMirror.toString());
                if (superElement != null) {
                    mergeSuperType(entity, superElement);
                    typeMirror = superElement.getSuperclass();
                } else {
                    break;
                }
            }
            // process the entity
            Set<ElementValidator> results = entity.process(processingEnv);
            validators.addAll(results);
        }
        for (EntityType entity : embeddedTypes.values()) {
            Set<ElementValidator> results = entity.process(processingEnv);
            validators.addAll(results);
        }
        for (EntityGraph graph : graphs.values()) {
            EntityGraphValidator validator = new EntityGraphValidator(processingEnv, graph);
            Set<ElementValidator> results = validator.validate();
            validators.addAll(results);
        }
        if (ElementValidator.hasErrors(validators)) {
            hasErrors = true;
            StringBuilder sb = new StringBuilder("Model has error(s) code generation may fail: ");
            validators.forEach(validator -> sb.append(validator.toString()));
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, sb);
        }

        // generate
        Set<SourceGenerator> generators = new LinkedHashSet<>();
        if (!hasErrors || getOption(GENERATE_ALWAYS, true)) {
            for (EntityDescriptor entity : entities.values()) {
                EntityGraph graph = graphs.get(entity.modelName());
                if (graph != null) {
                    generators.add(new EntityGenerator(processingEnv, graph, entity, null));
                }
            }
        }

        if (getOption(GENERATE_MODEL, true)) {
            Map<String, Collection<EntityDescriptor>> packagesMap = new LinkedHashMap<>();
            Map<String, Boolean> canGenerate = new HashMap<>();
            for (EntityType entity : entities.values()) {
                EntityGraph graph = graphs.get(entity.modelName());
                String packageName = findModelPackageName(graph);
                canGenerate.computeIfAbsent(packageName, key -> true);
                if (entity.generatesAdditionalTypes()) {
                    canGenerate.put(packageName, false);
                }
                if (packagesMap.containsKey(packageName)) {
                    packagesMap.get(packageName).addAll(graph.entities());
                } else {
                    packagesMap.put(packageName, new LinkedHashSet<>(graph.entities()));
                }
            }

            generators.addAll(
                packagesMap.entrySet().stream()
                    .filter(entry -> !entry.getValue().isEmpty())
                    .filter(entry -> canGenerate.get(entry.getKey()))
                    .map(entry ->
                        new ModelGenerator(processingEnv, entry.getKey(), entry.getValue()))
                    .collect(Collectors.toList()));
        }
        for (SourceGenerator generator : generators) {
            try {
                generator.generate();
            } catch (IOException e) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR, e.getMessage());
            }
        }
        return false;
    }

    private void mergeSuperType(EntityType entity, TypeElement superElement) {
        EntityType superType = superTypes.get(superElement);
        if (superType == null && isSuperclass(superElement)) {
            superType = computeType(superTypes, superElement);
        }
        if (superType != null) {
            superType.process(processingEnv);
            entity.merge(superType);
        }
    }

    private EntityType computeType(Map<TypeElement, EntityType> map, TypeElement element) {
        return map.computeIfAbsent(element, key -> new EntityType(processingEnv, key));
    }

    private boolean isEntity(TypeElement element) {
        return Mirrors.findAnnotationMirror(element, Entity.class).isPresent() ||
            (getOption(GENERATE_JPA, true) &&
             Mirrors.findAnnotationMirror(element, javax.persistence.Entity.class).isPresent());
    }

    private boolean isSuperclass(TypeElement element) {
        return Mirrors.findAnnotationMirror(element, Superclass.class).isPresent() ||
            (getOption(GENERATE_JPA, true) && Mirrors.findAnnotationMirror(element,
                javax.persistence.MappedSuperclass.class).isPresent());
    }

    private boolean isEmbeddable(TypeElement element) {
        return Mirrors.findAnnotationMirror(element, Embedded.class).isPresent() ||
            (getOption(GENERATE_JPA, true) && Mirrors.findAnnotationMirror(element,
                javax.persistence.Embeddable.class).isPresent());
    }

    private Optional<TypeElement> typeElementOf(Element element) {
        TypeElement typeElement = null;
        switch (element.getKind()) {
            case METHOD:
            case CONSTRUCTOR:
            case FIELD:
            case ENUM_CONSTANT:
                typeElement = (TypeElement) element.getEnclosingElement();
                break;
            case CLASS:
            case INTERFACE:
            case ENUM:
                typeElement = (TypeElement) element;
                break;
        }
        return Optional.ofNullable(typeElement);
    }

    private boolean getOption(String key, boolean defaultValue) {
        String value = processingEnv.getOptions().get(key);
        return value == null ? defaultValue : Boolean.valueOf(value);
    }

    private String findModelPackageName(EntityGraph graph) {
        String packageName = "";
        Set<String> packageNames = graph.entities().stream().map(
            entity -> entity.typeName().packageName()).collect(Collectors.toSet());

        if (packageNames.size() == 1) {
            // all the types are in the same package...
            packageName = packageNames.iterator().next();
        } else {
            String target = packageNames.iterator().next();

            while (target.indexOf(".") != target.lastIndexOf(".")) {
                target = target.substring(0, target.lastIndexOf("."));
                boolean allTypesInPackage = true;
                for (EntityDescriptor entity : graph.entities()) {
                    if (!entity.typeName().packageName().startsWith(target)) {
                        allTypesInPackage = false;
                    }
                }
                if (allTypesInPackage) {
                    packageName = target;
                    break;
                }
            }
            // no common package...
            if ("".equals(packageName)) {
                packageName = target;
            }
        }
        return packageName;
    }
}
