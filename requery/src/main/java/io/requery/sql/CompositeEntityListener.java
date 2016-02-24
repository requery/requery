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

package io.requery.sql;

import io.requery.proxy.EntityStateEventListeners;
import io.requery.proxy.PostLoadListener;
import io.requery.proxy.PostInsertListener;
import io.requery.proxy.PostDeleteListener;
import io.requery.proxy.PostUpdateListener;
import io.requery.proxy.PreInsertListener;
import io.requery.proxy.PreDeleteListener;
import io.requery.proxy.PreUpdateListener;
import io.requery.proxy.EntityProxy;

class CompositeEntityListener<T> extends EntityStateEventListeners<T> {

    private boolean enableStateListeners;

    public void enableStateListeners(boolean enabled) {
        this.enableStateListeners = enabled;
    }

    void preUpdate(T entity, EntityProxy<? extends T> proxy) {
        if (enableStateListeners) {
            for (PreUpdateListener<T> listener : preUpdateListeners) {
                listener.preUpdate(entity);
            }
        }
        if (proxy != null) {
            proxy.preUpdate();
        }
    }

    void postUpdate(T entity, EntityProxy<? extends T> proxy) {
        if (enableStateListeners) {
            for (PostUpdateListener<T> listener : postUpdateListeners) {
                listener.postUpdate(entity);
            }
        }
        if (proxy != null) {
            proxy.postUpdate();
        }
    }

    void preInsert(T entity, EntityProxy<? extends T> proxy) {
        if (enableStateListeners) {
            for (PreInsertListener<T> listener : preInsertListeners) {
                listener.preInsert(entity);
            }
        }
        if (proxy != null) {
            proxy.preInsert();
        }
    }

    void postInsert(T entity, EntityProxy<? extends T> proxy) {
        if (enableStateListeners) {
            for (PostInsertListener<T> listener : postInsertListeners) {
                listener.postInsert(entity);
            }
        }
        if (proxy != null) {
            proxy.postInsert();
        }
    }

    void preDelete(T entity, EntityProxy<? extends T> proxy) {
        if (enableStateListeners) {
            for (PreDeleteListener<T> listener : preDeleteListeners) {
                listener.preDelete(entity);
            }
        }
        if (proxy != null) {
            proxy.preDelete();
        }
    }

    void postDelete(T entity, EntityProxy<? extends T> proxy) {
        if (enableStateListeners) {
            for (PostDeleteListener<T> listener : postDeleteListeners) {
                listener.postDelete(entity);
            }
        }
        if (proxy != null) {
            proxy.postDelete();
        }
    }

    void postLoad(T entity, EntityProxy<? extends T> proxy) {
        if (enableStateListeners) {
            for (PostLoadListener<T> listener : postLoadListeners) {
                listener.postLoad(entity);
            }
        }
        if (proxy != null) {
            proxy.postLoad();
        }
    }
}
