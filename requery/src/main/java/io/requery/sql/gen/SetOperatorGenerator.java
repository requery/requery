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

package io.requery.sql.gen;

import io.requery.query.element.SetOperationElement;
import io.requery.sql.QueryBuilder;

import static io.requery.sql.Keyword.ALL;
import static io.requery.sql.Keyword.EXCEPT;
import static io.requery.sql.Keyword.INTERSECT;
import static io.requery.sql.Keyword.UNION;

class SetOperatorGenerator implements Generator<SetOperationElement> {

    @Override
    public void write(Output output, SetOperationElement query) {
        if(query.innerSetQuery() != null) {
            QueryBuilder qb = output.builder();
            switch (query.setOperator()) {
                case UNION:
                    qb.keyword(UNION);
                    break;
                case UNION_ALL:
                    qb.keyword(UNION, ALL);
                    break;
                case INTERSECT:
                    qb.keyword(INTERSECT);
                    break;
                case EXCEPT:
                    qb.keyword(EXCEPT);
                    break;
            }
            output.appendQuery(query.innerSetQuery());
        }
    }
}
