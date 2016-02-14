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

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.Arrays;
import java.util.Optional;

/**
 * Handler for Android's Observable objects. If the target entity object extends Observable
 * then the getter methods will be annotated with @Bindable and the setter methods will invoke
 * BaseObservable's notifyPropertyChanged method.
 *
 * @author Nikhil Purushe
 */
class AndroidObservableExtension implements TypeGenerationExtension, PropertyGenerationExtension {

    private static final String BR_ANNOTATION_NAME = "io.requery.android.BindingResource";
    private static final String BINDING_PACKAGE = "android.databinding";

    private final EntityDescriptor entity;
    private final ProcessingEnvironment processingEnvironment;
    private final boolean observable;

    AndroidObservableExtension(EntityDescriptor entity,
                               ProcessingEnvironment processingEnvironment) {
        this.entity = entity;
        this.processingEnvironment = processingEnvironment;
        this.observable = isObservable();
    }

    private boolean isObservable() {
        String[] types = new String[]{
            BINDING_PACKAGE + ".BaseObservable",
            BINDING_PACKAGE + ".Observable" };
        TypeElement element = entity.element();
        return Arrays.stream(types).anyMatch(name ->
            Mirrors.isInstance(processingEnvironment.getTypeUtils(), element, name));
    }

    @Override
    public void generate(EntityDescriptor entity, TypeSpec.Builder builder) {
        if (isObservable() && entity.element().getKind().isInterface()) {
            builder.superclass(ClassName.get(BINDING_PACKAGE, "BaseObservable"));
        }
    }

    @Override
    public void addToGetter(AttributeDescriptor member, MethodSpec.Builder builder) {
        if (observable) {
            builder.addAnnotation(ClassName.get(BINDING_PACKAGE, "Bindable"));
        }
    }

    @Override
    public void addToSetter(AttributeDescriptor member, MethodSpec.Builder builder) {
        if (!observable) {
            return;
        }
        Elements elements = processingEnvironment.getElementUtils();
        // since we don't know BR package exactly class must be annotated with @BindingResource
        // in those cases
        Optional<? extends AnnotationMirror> mirror =
            Mirrors.findAnnotationMirror(entity.element(), BR_ANNOTATION_NAME);

        ClassName BRclass;
        if (mirror.isPresent()) {
            Object value = Mirrors.findAnnotationValue(mirror.get())
                .map(AnnotationValue::getValue).map(Object::toString).get();
            BRclass = ClassName.bestGuess(value.toString());
        } else {
            PackageElement packageElement = elements.getPackageOf(entity.element());
            String packageName = packageElement.getQualifiedName().toString();
            BRclass = ClassName.get(packageName, "BR");
        }

        // matching the way the BR property names are generated
        // https://code.google.com/p/android/issues/detail?id=199436
        String propertyName = member.setterName().replaceFirst("set", "");
        /*
        if (Names.isAllUpper(propertyName)) {
            propertyName = propertyName.toLowerCase();
        } else {
            propertyName = Names.lowerCaseFirst(propertyName);
        }*/
        propertyName = Names.lowerCaseFirst(propertyName);
        builder.addStatement("notifyPropertyChanged($L.$L)", BRclass, propertyName);
    }
}
