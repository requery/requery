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

package io.requery.proxy;

/**
 * Listenable for indvidual states of an entity.
 *
 * @param <T> type of entity
 */
public interface EntityStateEventListenable<T> {

    void addPreInsertListener(PreInsertListener<T> listener);

    void addPreDeleteListener(PreDeleteListener<T> listener);

    void addPreUpdateListener(PreUpdateListener<T> listener);

    void addPostInsertListener(PostInsertListener<T> listener);

    void addPostDeleteListener(PostDeleteListener<T> listener);

    void addPostUpdateListener(PostUpdateListener<T> listener);

    void addPostLoadListener(PostLoadListener<T> listener);

    void removePreInsertListener(PreInsertListener<T> listener);

    void removePreDeleteListener(PreDeleteListener<T> listener);

    void removePreUpdateListener(PreUpdateListener<T> listener);

    void removePostInsertListener(PostInsertListener<T> listener);

    void removePostDeleteListener(PostDeleteListener<T> listener);

    void removePostUpdateListener(PostUpdateListener<T> listener);

    void removePostLoadListener(PostLoadListener<T> listener);
}
