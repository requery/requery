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

class CompositeEntityStateListener<T> extends EntityStateEventListeners<T> implements
    EntityStateListener {

    private final T entity;

    CompositeEntityStateListener(T entity) {
        this.entity = entity;
    }

    @Override
    public void preUpdate() {
        for (PreUpdateListener<T> listener : preUpdateListeners) {
            listener.preUpdate(entity);
        }
    }

    @Override
    public void postUpdate() {
        for (PostUpdateListener<T> listener : postUpdateListeners) {
            listener.postUpdate(entity);
        }
    }

    @Override
    public void preInsert() {
        for (PreInsertListener<T> listener : preInsertListeners) {
            listener.preInsert(entity);
        }
    }

    @Override
    public void postInsert() {
        for (PostInsertListener<T> listener : postInsertListeners) {
            listener.postInsert(entity);
        }
    }

    @Override
    public void preDelete() {
        for (PreDeleteListener<T> listener : preDeleteListeners) {
            listener.preDelete(entity);
        }
    }

    @Override
    public void postDelete() {
        for (PostDeleteListener<T> listener : postDeleteListeners) {
            listener.postDelete(entity);
        }
    }

    @Override
    public void postLoad() {
        for (PostLoadListener<T> listener : postLoadListeners) {
            listener.postLoad(entity);
        }
    }
}
