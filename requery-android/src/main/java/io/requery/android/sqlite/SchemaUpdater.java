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

package io.requery.android.sqlite;

import android.database.Cursor;
import io.requery.meta.Attribute;
import io.requery.meta.Type;
import io.requery.sql.Configuration;
import io.requery.sql.SchemaModifier;
import io.requery.sql.TableCreationMode;
import io.requery.util.function.Function;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Basic schema updater that adds missing tables and columns.
 */
public class SchemaUpdater {

    private final Configuration configuration;
    private final Function<String, Cursor> queryFunction;
    private final TableCreationMode mode;

    public SchemaUpdater(Configuration configuration,
                         Function<String, Cursor> queryFunction,
                         TableCreationMode mode) {
        this.configuration = configuration;
        this.queryFunction = queryFunction;
        this.mode = mode == null ? TableCreationMode.CREATE_NOT_EXISTS : mode;
    }

    public void update() {
        SchemaModifier schema = new SchemaModifier(configuration);
        schema.createTables(mode);
        if (mode == TableCreationMode.DROP_CREATE) {
            return; // don't need to check missing columns
        }
        // check for missing columns
        List<Attribute> missingAttributes = new ArrayList<>();
        for (Type<?> type : configuration.getModel().getTypes()) {
            if (type.isView()) {
                continue;
            }
            String tableName = type.getName();
            Cursor cursor = queryFunction.apply("PRAGMA table_info(" + tableName + ")");
            Map<String, Attribute> map = new LinkedHashMap<>();
            for (Attribute attribute : type.getAttributes()) {
                if (attribute.isAssociation() && !attribute.isForeignKey()) {
                    continue;
                }
                map.put(attribute.getName(), attribute);
            }
            if (cursor.getCount() > 0) {
                int nameIndex = cursor.getColumnIndex("name");
                while (cursor.moveToNext()) {
                    String name = cursor.getString(nameIndex);
                    map.remove(name);
                }
            }
            cursor.close();
            // whats left in the map are are the missing columns for this type
            missingAttributes.addAll(map.values());
        }
        // foreign keys are created last
        Collections.sort(missingAttributes, new Comparator<Attribute>() {
            @Override
            public int compare(Attribute lhs, Attribute rhs) {
                if (lhs.isForeignKey() && rhs.isForeignKey()) {
                    return 0;
                }
                if (lhs.isForeignKey()) {
                    return 1;
                }
                return -1;
            }
        });
        for (Attribute<?, ?> attribute : missingAttributes) {
            schema.addColumn(attribute);
        }
    }
}
