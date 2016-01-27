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

package io.requery;

/**
 * Cascade action to use when adding or removing an element to an associative collection. Note this
 * is different from {@link ReferentialAction} which is specifically for a {@link ForeignKey}
 * constraint. This enumeration controls how entities in relational associations are persisted and
 * updated when their containing entity is modified.
 */
public enum CascadeAction {

    /**
     * When a associative element is added no action will be taken, this element must be persisted
     * before the object referencing it is persisted.
     */
    NONE,

    /**
     * When a associative element is added it will be persisted (inserted or updated) if required.
     */
    SAVE,

    /**
     * When the parent element is deleted its associated elements are also deleted.
     */
    DELETE
}
