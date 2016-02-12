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

import java.util.LinkedHashSet;
import java.util.Set;

public class EntityStateEventListeners<T> implements EntityStateEventListenable<T> {

    protected final Set<PreInsertListener<T>> preInsertListeners;
    protected final Set<PreDeleteListener<T>> preDeleteListeners;
    protected final Set<PreUpdateListener<T>> preUpdateListeners;
    protected final Set<PostInsertListener<T>> postInsertListeners;
    protected final Set<PostDeleteListener<T>> postDeleteListeners;
    protected final Set<PostUpdateListener<T>> postUpdateListeners;
    protected final Set<PostLoadListener<T>> postLoadListeners;

    protected EntityStateEventListeners() {
        preInsertListeners = new LinkedHashSet<>();
        preDeleteListeners = new LinkedHashSet<>();
        preUpdateListeners = new LinkedHashSet<>();
        postInsertListeners = new LinkedHashSet<>();
        postDeleteListeners = new LinkedHashSet<>();
        postUpdateListeners = new LinkedHashSet<>();
        postLoadListeners = new LinkedHashSet<>();
    }

    @Override
    public void addPreInsertListener(PreInsertListener<T> listener) {
        preInsertListeners.add(listener);
    }

    @Override
    public void addPreDeleteListener(PreDeleteListener<T> listener) {
        preDeleteListeners.add(listener);
    }

    @Override
    public void addPreUpdateListener(PreUpdateListener<T> listener) {
        preUpdateListeners.add(listener);
    }

    @Override
    public void addPostInsertListener(PostInsertListener<T> listener) {
        postInsertListeners.add(listener);
    }

    @Override
    public void addPostDeleteListener(PostDeleteListener<T> listener) {
        postDeleteListeners.add(listener);
    }

    @Override
    public void addPostUpdateListener(PostUpdateListener<T> listener) {
        postUpdateListeners.add(listener);
    }

    @Override
    public void addPostLoadListener(PostLoadListener<T> listener) {
        postLoadListeners.add(listener);
    }

    @Override
    public void removePreInsertListener(PreInsertListener<T> listener) {
        preInsertListeners.remove(listener);
    }

    @Override
    public void removePreDeleteListener(PreDeleteListener<T> listener) {
        preDeleteListeners.remove(listener);
    }

    @Override
    public void removePreUpdateListener(PreUpdateListener<T> listener) {
        preUpdateListeners.remove(listener);
    }

    @Override
    public void removePostInsertListener(PostInsertListener<T> listener) {
        postInsertListeners.remove(listener);
    }

    @Override
    public void removePostDeleteListener(PostDeleteListener<T> listener) {
        postDeleteListeners.remove(listener);
    }

    @Override
    public void removePostUpdateListener(PostUpdateListener<T> listener) {
        postUpdateListeners.remove(listener);
    }

    @Override
    public void removePostLoadListener(PostLoadListener<T> listener) {
        postLoadListeners.remove(listener);
    }
}
