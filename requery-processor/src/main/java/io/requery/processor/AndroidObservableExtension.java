/*
 * Copyright 2018 requery.io
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
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;

/**
 * Handler for Android's Observable objects. If the target entity object extends Observable
 * then the getter methods will be annotated with @Bindable and the setter methods will invoke
 * BaseObservable's notifyPropertyChanged method.
 *
 * @author Nikhil Purushe
 */
class AndroidObservableExtension implements TypeGenerationExtension, PropertyGenerationExtension {

    private static final String BINDING_PACKAGE = "androidx.databinding";
    private static final String MODULE_PACKAGE_OPTION = "android.databinding.modulePackage";

    private final EntityDescriptor entity;
    private final ProcessingEnvironment processingEnvironment;
    private final boolean observable;
    private String modulePackage;

    AndroidObservableExtension(EntityDescriptor entity,
                               ProcessingEnvironment processingEnvironment) {
        this.entity = entity;
        this.processingEnvironment = processingEnvironment;
        this.observable = isObservable();
        this.modulePackage = processingEnvironment.getOptions().get(MODULE_PACKAGE_OPTION);

        // this shouldn't be happening
        if (modulePackage == null) {
            for(Map.Entry<String, String> entry : processingEnvironment.getOptions().entrySet()) {
                if (entry.getKey().endsWith("databinding.modulePackage")) {
                    modulePackage = entry.getValue();
                    break;
                }
            }
        }
    }

    private boolean isObservable() {
        String[] types = new String[]{
            BINDING_PACKAGE + ".BaseObservable",
            BINDING_PACKAGE + ".Observable" };
        TypeElement element = entity.element();
        return Arrays.stream(types).anyMatch(name ->
            Mirrors.isInstance(processingEnvironment.getTypeUtils(), element, name));
    }

    private boolean isBindable(AttributeDescriptor descriptor) {
        Element element = descriptor.element();
        return Mirrors.findAnnotationMirror(element, BINDING_PACKAGE + ".Bindable").isPresent();
    }

    @Override
    public void generate(EntityDescriptor entity, TypeSpec.Builder builder) {
        if (isObservable() && entity.element().getKind().isInterface()) {
            builder.superclass(ClassName.get(BINDING_PACKAGE, "BaseObservable"));
        }
    }

    @Override
    public void addToGetter(AttributeDescriptor member, MethodSpec.Builder builder) {
        if (observable && isBindable(member)) {
            builder.addAnnotation(ClassName.get(BINDING_PACKAGE, "Bindable"));
        }
    }

    @Override
    public void addToSetter(AttributeDescriptor member, MethodSpec.Builder builder) {
        if (!observable || !isBindable(member)) {
            return;
        }
        Elements elements = processingEnvironment.getElementUtils();

        // data binding compiler will create a useful set of classes in /data-binding-info
        String bindingInfo = BINDING_PACKAGE + ".layouts.DataBindingInfo";
        TypeElement dataBindingType = elements.getTypeElement(bindingInfo);
        ClassName BRclass = null;

        if (!Names.isEmpty(modulePackage)) {
            BRclass = ClassName.get(modulePackage, "BR");
        } else if (dataBindingType != null) {
            // fallback for pre Android gradle plugin 2.3.0
            Optional<String> modulePackage = Mirrors.findAnnotationMirror(
                dataBindingType, BINDING_PACKAGE + ".BindingBuildInfo")
            .map(mirror -> Mirrors.findAnnotationValue(mirror, "modulePackage"))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .map(AnnotationValue::toString);
            if (modulePackage.isPresent()) {
                // not actually checking the BR class exists since it maybe generated later
                BRclass = ClassName.get(modulePackage.get().replaceAll("\"", ""), "BR");
            }
        }
        if (BRclass == null) {
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
