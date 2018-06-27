/*
 * Copyright 2018 requery.io
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

package io.requery.android.sqlite

import android.database.Cursor
import io.requery.meta.Attribute
import io.requery.sql.Configuration
import io.requery.sql.SchemaModifier
import io.requery.sql.TableCreationMode
import io.requery.sql.TableModificationException
import io.requery.util.function.Function

import java.sql.Connection
import java.sql.SQLException
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.LinkedHashMap

/**
 * Basic schema updater that adds missing tables and columns.
 */
class SchemaUpdater(private val configuration: Configuration,
                    private val queryFunction: Function<String, Cursor>,
                    mode: TableCreationMode?) {

    private val mode: TableCreationMode = mode ?: TableCreationMode.CREATE_NOT_EXISTS

    fun update() {
        val schema = SchemaModifier(configuration)
        if (mode == TableCreationMode.DROP_CREATE) {
            schema.createTables(mode) // don't need to check missing columns
        } else {
            try {
                schema.connection.use { connection ->
                    connection.autoCommit = false
                    upgrade(connection, schema)
                    connection.commit()
                }
            } catch (e: SQLException) {
                throw TableModificationException(e)
            }

        }
    }

    private fun upgrade(connection: Connection, schema: SchemaModifier) {
        schema.createTables(connection, mode, false)
        val columnTransformer = configuration.columnTransformer
        val tableTransformer = configuration.tableTransformer
        // check for missing columns
        val missingAttributes = ArrayList<Attribute<*, *>>()
        for (type in configuration.model.types) {
            if (type.isView) {
                continue
            }
            var tableName = type.name
            if (tableTransformer != null) {
                tableName = tableTransformer.apply(tableName)
            }
            val cursor = queryFunction.apply("PRAGMA table_info($tableName)")
            val map = LinkedHashMap<String, Attribute<*, *>>()
            for (attribute in type.attributes) {
                if (attribute.isAssociation && !attribute.isForeignKey) {
                    continue
                }
                if (columnTransformer == null) {
                    map[attribute.name] = attribute
                } else {
                    map[columnTransformer.apply(attribute.name)] = attribute
                }
            }
            if (cursor.count > 0) {
                val nameIndex = cursor.getColumnIndex("name")
                while (cursor.moveToNext()) {
                    val name = cursor.getString(nameIndex)
                    map.remove(name)
                }
            }
            cursor.close()
            // whats left in the map are are the missing columns for this type
            missingAttributes.addAll(map.values)
        }
        // foreign keys are created last
        Collections.sort(missingAttributes, Comparator<Attribute<*, *>> { lhs, rhs ->
            if (lhs.isForeignKey && rhs.isForeignKey) {
                return@Comparator 0
            }
            if (lhs.isForeignKey) {
                1
            } else -1
        })
        for (attribute in missingAttributes) {
            schema.addColumn(connection, attribute, false)
            if (attribute.isUnique && !attribute.isIndexed) {
                schema.createIndex(connection, attribute, mode)
            }
        }
        schema.createIndexes(connection, mode)
    }
}
