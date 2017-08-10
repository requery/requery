/*
 * Copyright 2017 requery.io
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

package io.requery.jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import io.requery.meta.EntityModel;

public class RequeryModule extends Module {

    private static final Version VERSION = new Version(1,0,0, "", "io.requery", "requery-jackson");
    private final EntityModel model;

    protected RequeryModule(EntityModel model) {
        this.model = model;
    }

    @Override
    public String getModuleName() {
        return "requery";
    }

    @Override
    public Version version() {
        return VERSION;
    }

    @Override
    public void setupModule(SetupContext context) {
        context.addBeanDeserializerModifier(new DeserializerModifier());
        context.appendAnnotationIntrospector(new EntityAnnotationIntrospector(model, version()));
    }
}
