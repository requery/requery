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

import static io.requery.sql.Keyword.*;

public class IdentityColumnDefinition implements GeneratedColumnDefinition {

    @Override
    public boolean skipTypeIdentifier() {
        return false;
    }

    @Override
    public boolean postFixPrimaryKey() {
        return false;
    }

    @Override
    public void appendGeneratedSequence(QueryBuilder qb, Attribute attribute) {
        int start = 1;
        int increment = 1;
        qb.keyword(GENERATED, ALWAYS, AS, IDENTITY);
        qb.openParenthesis()
                .keyword(START, WITH).value(start)
                .comma()
                .keyword(INCREMENT, BY).value(increment)
                .closeParenthesis()
                .space();
    }
}
