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

import java.io.Serializable;
import java.util.Arrays;

/**
 * Represents a composite key of multiple values.
 *
 * @author Nikhil Purushe
 */
public class CompositeKey implements Serializable {

    private final Object[] values;

    public CompositeKey(Object[] values) {
        this.values = values;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof CompositeKey) {
            CompositeKey other = (CompositeKey) obj;
            return Arrays.equals(values, other.values);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(values);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        int index = 0;
        for (Object o : values) {
            if (index > 0) {
                sb.append(", ");
            }
            index++;
            sb.append(o.toString());
        }
        sb.append("]");
        return sb.toString();
    }
}
