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

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import io.requery.EntityStore;
import io.requery.query.Result;

public abstract class QueryLoader<E> extends AsyncTaskLoader<Result<E>> {

    private final EntityStore data;
    private Result<E> result;

    public QueryLoader(EntityStore data, Context context) {
        super(context);
        this.data = data;
    }

    @Override
    protected void onStartLoading() {
        if (result != null) {
            deliverResult(result);
        } else {
            forceLoad();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();
        onStopLoading();
        if (result != null) {
            result.close();
            result = null;
        }
    }

    @Override
    public Result<E> loadInBackground() {
        return performQuery(data);
    }

    public abstract Result<E> performQuery(EntityStore data);

    @Override
    public void deliverResult(Result<E> data) {
        if (isReset()) {
            if (result != null) {
                result.close();
            }
            return;
        }
        Result<E> previous = result;
        result = data;
        if (isStarted()) {
            super.deliverResult(result);
        }
        if (previous != null) {
            previous.close();
        }
    }
}
