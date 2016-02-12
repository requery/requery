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

package io.requery.query.function;

import java.sql.Date;

public class Now<T> extends Function<T> {

    public Now(Class<T> type) {
        super("now", type);
    }

    public static Now<Date> now() {
        return new Now<>(Date.class);
    }

    public static <T> Now<T> now(Class<T> type) {
        return new Now<>(type);
    }

    @Override
    public Object[] arguments() {
        return new Object[0];
    }
}
