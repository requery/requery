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

package io.requery.meta;

import java.util.Set;

public final class Types {

    private Types() {
    }

    public static boolean referencesType(Set<Type<?>> source, Set<Type<?>> changed) {
        for (Type<?> type : source) {
            for (Attribute<?, ?> attribute : type.getAttributes()) {
                // find if any referencing types that maybe affected by changes to the type
                if (attribute.isAssociation()) {
                    Attribute referenced = null;
                    if (attribute.getReferencedAttribute() != null) {
                        referenced = attribute.getReferencedAttribute().get();
                    }
                    if (attribute.getMappedAttribute() != null) {
                        referenced = attribute.getMappedAttribute().get();
                    }
                    if (referenced != null) {
                        Type<?> declared = referenced.getDeclaringType();
                        if (changed.contains(declared)) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }
}
