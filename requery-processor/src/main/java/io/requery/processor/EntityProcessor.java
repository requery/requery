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

import io.requery.Entity;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
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

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        graphs = new LinkedHashMap<>();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // types to generate in this round
        Map<TypeElement, EntityType> entities = new HashMap<>();
        Types types = processingEnv.getTypeUtils();

        for (TypeElement annotation : annotations) {
            for (Element element : roundEnv.getElementsAnnotatedWith(annotation)) {
                Optional<TypeElement> typeElement = typeElementOf(element);

                if (typeElement.isPresent() &&
                    typeElement.map(this::isTypeProcessable).orElse(false)) {
                    // create or get the entity for the annotation
                    EntityType entity = entities.computeIfAbsent(typeElement.get(),
                        key -> new EntityType(processingEnv, key));
                    entity.addAnnotationElement(annotation, element);
                    // create or get the graph for it
                    String key = entity.modelName();
                    graphs.computeIfAbsent(key, k -> new EntityGraph(types)).add(entity);
                }
            }
        }
        // process
        boolean hasErrors = false;
        Set<ElementValidator> validators = new LinkedHashSet<>();
        for (EntityType entity : entities.values()) {
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
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                "model has errors code generation may fail");
        }

        // generate
        Set<SourceGenerator> generators = new LinkedHashSet<>();
        if (!hasErrors || getBooleanOption(GENERATE_ALWAYS, true)) {
            for (EntityDescriptor entity : entities.values()) {
                EntityGraph graph = graphs.get(entity.modelName());
                if (graph != null) {
                    generators.add(new EntityGenerator(processingEnv, graph, entity));
                }
            }
        }

        if (getBooleanOption(GENERATE_MODEL, true)) {
            boolean canGenerateModel = true;
            Map<String, Collection<EntityDescriptor>> packagesMap = new LinkedHashMap<>();
            for (Map.Entry<String, EntityGraph> entry : graphs.entrySet()) {
                EntityGraph graph = entry.getValue();
                for (EntityType entity : entities.values()) {
                    if (graph.entities().contains(entity) &&
                        entity.generatesAdditionalTypes()) {

                        canGenerateModel = false;
                    }
                }
                if (!entities.isEmpty() && canGenerateModel) {
                    String packageName = findModelPackageName(graph);
                    if (packagesMap.containsKey(packageName)) {
                        packagesMap.get(packageName).addAll(graph.entities());
                    } else {
                        packagesMap.put(packageName, new LinkedHashSet<>(graph.entities()));
                    }
                }
            }
            generators.addAll(
                packagesMap.entrySet().stream()
                    .filter(entry -> !entry.getValue().isEmpty())
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

    private boolean isTypeProcessable(TypeElement element) {
        return Mirrors.findAnnotationMirror(element, Entity.class).isPresent() ||
            (getBooleanOption(GENERATE_JPA, true) &&
             Mirrors.findAnnotationMirror(element, javax.persistence.Entity.class).isPresent());
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

    private boolean getBooleanOption(String key, boolean defaultValue) {
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
                packageName = "io.requery.generated";
            }
        }
        return packageName;
    }
}
