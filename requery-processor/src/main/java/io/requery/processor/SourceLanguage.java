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

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Represents the source language type an annotation definition is present in. Besides Java other
 * JVM languages can implement support for annotation processing. (Currently only Kotlin is
 * supported)
 *
 * @author Nikhil Purushe
 */
enum SourceLanguage {

    JAVA,
    KOTLIN;

    private static final Map<TypeElement, SourceLanguage> map = new LinkedHashMap<>();
    private static final Set<TypeElement> annotations = new LinkedHashSet<>();

    static SourceLanguage of(TypeElement element) {
        if (map.containsKey(element)) {
            return map.get(element);
        }
        return SourceLanguage.JAVA;
    }

    static void map(ProcessingEnvironment environment) {
        map.clear();
        annotations.clear();
        try {
            readKaptTypes(environment, map);
        } catch (IOException e) {
            environment.getMessager().printMessage(Diagnostic.Kind.WARNING, e.toString());
        }
    }

    protected static Set<TypeElement> getAnnotations() {
        return annotations;
    }

    // reads the file specified in kapt.annotations determine if a specific type element is from
    // a Kotlin source file
    private static void readKaptTypes(ProcessingEnvironment environment,
                                      Map<TypeElement, SourceLanguage> map) throws IOException {
        String path = environment.getOptions().get("kapt.annotations");
        if (path == null) {
            return;
        }
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        try (FileInputStream input = new FileInputStream(file);
             BufferedReader buffered = new BufferedReader(new InputStreamReader(input))) {
            String line;
            String annotationType = null;
            String packageName = null;
            String classType;
            while ((line = buffered.readLine()) != null) {
                classType = null; // reset
                String[] parts = line.split(" ");
                switch (parts[0]) {
                    case "p":
                        packageName = parts[1];
                        break;
                    case "a":
                        annotationType = parts[1];
                        break;
                    case "c": // c 0 0/Example
                        classType = parts[2];
                        break;
                    default: break;
                    // don't care about methods/fields at the moment
                }
                if (classType != null && annotationType != null) {
                    parts = classType.split("/");
                    String name = parts[parts.length - 1];
                    String qname = packageName + "." + name; // not supporting nested classes
                    TypeElement element = environment.getElementUtils().getTypeElement(qname);
                    if (element != null) {
                        environment.getMessager().printMessage(Diagnostic.Kind.OTHER,
                                "kapt @ " + annotationType + " on " + element.getQualifiedName());
                        Elements elements = environment.getElementUtils();
                        TypeElement annotationElement = elements.getTypeElement(annotationType);
                        if (annotationElement != null) {
                            annotations.add(annotationElement);
                        }
                        map.put(element, SourceLanguage.KOTLIN);
                    }
                }
            }
        }
    }
}
