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

package io.requery.android;

/**
 * When using Android databinding and BaseObservables use this annotation to provide the processor
 * the location of the BR class so that appropriate change notifications can be triggered.
 */
public @interface BindingResource {

    /**
     * @return fully qualified class name of the BR class.
     */
    String value();
}
