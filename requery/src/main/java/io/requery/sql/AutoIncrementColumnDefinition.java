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

import io.requery.meta.Attribute;

public class AutoIncrementColumnDefinition implements GeneratedColumnDefinition {

    private final String autoincrementKeyword;

    public AutoIncrementColumnDefinition() {
        this("auto_increment");
    }

    public AutoIncrementColumnDefinition(String autoincrementKeyword) {
        this.autoincrementKeyword = autoincrementKeyword;
    }

    @Override
    public boolean skipTypeIdentifier() {
        return false;
    }

    @Override
    public boolean postFixPrimaryKey() {
        return true;
    }

    @Override
    public void appendGeneratedSequence(QueryBuilder qb, Attribute attribute) {
        qb.append(autoincrementKeyword);
    }
}
