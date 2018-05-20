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
import android.database.CursorWrapper
import android.database.sqlite.SQLiteDatabase
import io.requery.sql.Keyword
import io.requery.sql.QueryBuilder
import io.requery.util.function.Function

import java.io.Closeable
import java.io.IOException
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet
import java.sql.RowIdLifetime
import java.sql.SQLException
import java.util.Arrays
import java.util.LinkedHashMap

open class SqliteMetaData(private val connection: BaseConnection) : DatabaseMetaData {

    @Throws(SQLException::class)
    protected open fun <R> queryMemory(function: Function<Cursor, R>, query: String): R {
        try {
            val database = SQLiteDatabase.openOrCreateDatabase(":memory:", null)
            val cursor = database.rawQuery(query, null)
            return function.apply(closeWithCursor(database, cursor))
        } catch (e: android.database.SQLException) {
            throw SQLException(e)
        }

    }

    protected fun closeWithCursor(closeable: Closeable?, cursor: Cursor): CursorWrapper {
        return object : CursorWrapper(cursor) {
            override fun close() {
                super.close()
                if (closeable != null) {
                    try {
                        closeable.close()
                    } catch (ignored: IOException) {
                    }
                }
            }
        }
    }

    override fun allProceduresAreCallable(): Boolean = false

    override fun allTablesAreSelectable(): Boolean = true

    override fun dataDefinitionCausesTransactionCommit(): Boolean = false

    override fun dataDefinitionIgnoredInTransactions(): Boolean = false

    override fun deletesAreDetected(type: Int): Boolean = false

    override fun doesMaxRowSizeIncludeBlobs(): Boolean = false

    override fun getAttributes(catalog: String, schemaPattern: String, typeNamePattern: String, attributeNamePattern: String): ResultSet? = null

    override fun getBestRowIdentifier(catalog: String, schema: String, table: String, scope: Int, nullable: Boolean): ResultSet? = null

    override fun getCatalogs(): ResultSet? = null

    override fun getCatalogSeparator(): String? = null

    override fun getCatalogTerm(): String? = null

    override fun getColumnPrivileges(catalog: String, schema: String, table: String, columnNamePattern: String): ResultSet? = null

    override fun getColumns(catalog: String, schemaPattern: String, tableNamePattern: String, columnNamePattern: String): ResultSet? = null

    override fun getConnection(): Connection = connection

    override fun getCrossReference(primaryCatalog: String, primarySchema: String, primaryTable: String, foreignCatalog: String, foreignSchema: String, foreignTable: String): ResultSet? = null

    @Throws(SQLException::class)
    override fun getDatabaseMajorVersion(): Int {
        return Integer.parseInt(databaseProductVersion.split(".".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0])
    }

    @Throws(SQLException::class)
    override fun getDatabaseMinorVersion(): Int {
        return Integer.parseInt(databaseProductVersion.split(".".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[1])
    }

    override fun getDatabaseProductName(): String = "SQLite"

    @Throws(SQLException::class)
    override fun getDatabaseProductVersion(): String {
        return queryMemory(Function { cursor ->
            var version = ""
            if (cursor.moveToNext()) {
                version = cursor.getString(0)
            }
            version
        }, "select sqlite_version() AS sqlite_version")
    }

    override fun getDefaultTransactionIsolation(): Int = Connection.TRANSACTION_READ_COMMITTED

    override fun getDriverMajorVersion(): Int = 1

    override fun getDriverMinorVersion(): Int = 0

    override fun getDriverName(): String = "SQLite Android"

    override fun getDriverVersion(): String = "1.0"

    override fun getExportedKeys(catalog: String, schema: String, table: String): ResultSet? = null

    override fun getExtraNameCharacters(): String = ""

    override fun getIdentifierQuoteString(): String = "\""

    override fun getImportedKeys(catalog: String, schema: String, table: String): ResultSet? = null

    override fun getIndexInfo(catalog: String, schema: String, table: String, unique: Boolean, approximate: Boolean): ResultSet? = null

    override fun getJDBCMajorVersion(): Int = 1

    override fun getJDBCMinorVersion(): Int = 0

    override fun getMaxBinaryLiteralLength(): Int = 0

    override fun getMaxCatalogNameLength(): Int = 0

    override fun getMaxCharLiteralLength(): Int = 0

    override fun getMaxColumnNameLength(): Int = 1000000000

    override fun getMaxColumnsInGroupBy(): Int = 500

    override fun getMaxColumnsInIndex(): Int = 500

    override fun getMaxColumnsInOrderBy(): Int = 500

    override fun getMaxColumnsInSelect(): Int = 500

    override fun getMaxColumnsInTable(): Int = 2000

    override fun getMaxConnections(): Int = 1

    override fun getMaxCursorNameLength(): Int = 0

    override fun getMaxIndexLength(): Int = 0

    override fun getMaxProcedureNameLength(): Int = 0

    override fun getMaxRowSize(): Int = 0

    override fun getMaxSchemaNameLength(): Int = 1000000

    override fun getMaxStatementLength(): Int = 1000000

    override fun getMaxStatements(): Int = 0

    override fun getMaxTableNameLength(): Int = 1000000

    override fun getMaxTablesInSelect(): Int = 500

    override fun getMaxUserNameLength(): Int = 0

    override fun getNumericFunctions(): String = "abs,hex,max,min,random"

    override fun getPrimaryKeys(catalog: String, schema: String, table: String): ResultSet? = null

    override fun getProcedureColumns(catalog: String, schemaPattern: String, procedureNamePattern: String, columnNamePattern: String): ResultSet? = null

    override fun getProcedures(catalog: String, schemaPattern: String, procedureNamePattern: String): ResultSet? = null

    override fun getProcedureTerm(): String? = null

    override fun getResultSetHoldability(): Int = ResultSet.HOLD_CURSORS_OVER_COMMIT

    override fun getSchemas(): ResultSet? = null

    override fun getSchemaTerm(): String? = null

    override fun getSearchStringEscape(): String? = null

    override fun getSQLKeywords(): String = ""

    override fun getSQLStateType(): Int = DatabaseMetaData.sqlStateSQL99

    override fun getStringFunctions(): String = ""

    override fun getSuperTables(catalog: String, schemaPattern: String, tableNamePattern: String): ResultSet? = null

    override fun getSuperTypes(catalog: String, schemaPattern: String, typeNamePattern: String): ResultSet? = null

    override fun getSystemFunctions(): String = ""

    override fun getTablePrivileges(catalog: String, schemaPattern: String, tableNamePattern: String): ResultSet? = null

    @Throws(SQLException::class)
    override fun getTables(catalog: String, schemaPattern: String, tableNamePattern: String?, types: Array<String>?): ResultSet {
        var tableNamePattern = tableNamePattern
        var types = types
        if (types == null) {
            types = arrayOf("TABLE", "VIEW")
        }
        if (tableNamePattern == null) {
            tableNamePattern = "%"
        }
        val select = LinkedHashMap<String, String?>()
        select["TABLE_CAT"] = null
        select["TABLE_SCHEM"] = null
        select["TABLE_NAME"] = "name"
        select["TABLE_TYPE"] = "type"
        select["REMARKS"] = null
        select["TYPE_CAT"] = null
        select["TYPE_SCHEM"] = null
        select["TYPE_NAME"] = null
        select["SELF_REFERENCING_COL_NAME"] = null
        select["REF_GENERATION"] = null
        val qb = QueryBuilder(
                QueryBuilder.Options(identifierQuoteString, true, null, null, false, false))
                .keyword(Keyword.SELECT)
                .commaSeparated<Map.Entry<String, String?>>(select.entries
                ) { qb, entry ->
                    val value = if (entry.value == null) "null" else entry.value
                    qb.append(value).append(" as ").append(entry.key)
                }
                .keyword(Keyword.FROM)
                .openParenthesis().append("select name, type from sqlite_master").closeParenthesis()
                .keyword(Keyword.WHERE)
                .append(" TABLE_NAME like ").append(tableNamePattern).append(" && TABLE_TYPE in ")
                .openParenthesis()
                .commaSeparated(Arrays.asList(*types))
                .closeParenthesis()
                .keyword(Keyword.ORDER, Keyword.BY)
                .append(" TABLE_TYPE, TABLE_NAME")
        return queryMemory(Function { cursor -> CursorResultSet(null, cursor, true) }, qb.toString())
    }

    @Throws(SQLException::class)
    override fun getTableTypes(): ResultSet {
        return queryMemory(Function { cursor -> CursorResultSet(null, cursor, true) }, "select 'TABLE' as TABLE_TYPE, 'VIEW' as TABLE_TYPE")
    }

    override fun getTimeDateFunctions(): String? = null

    override fun getTypeInfo(): ResultSet? = null

    override fun getUDTs(catalog: String, schemaPattern: String, typeNamePattern: String, types: IntArray): ResultSet? = null

    override fun getURL(): String? = null

    override fun getUserName(): String? = null

    override fun getVersionColumns(catalog: String, schema: String, table: String): ResultSet? = null

    override fun insertsAreDetected(type: Int): Boolean = false

    override fun isCatalogAtStart(): Boolean = false

    @Throws(SQLException::class)
    override fun isReadOnly(): Boolean = connection.isReadOnly

    override fun locatorsUpdateCopy(): Boolean = false

    override fun nullPlusNonNullIsNull(): Boolean = true

    override fun nullsAreSortedAtEnd(): Boolean = false

    override fun nullsAreSortedAtStart(): Boolean = true

    override fun nullsAreSortedHigh(): Boolean = false

    override fun nullsAreSortedLow(): Boolean = false

    override fun othersDeletesAreVisible(type: Int): Boolean = false

    override fun othersInsertsAreVisible(type: Int): Boolean = false

    override fun othersUpdatesAreVisible(type: Int): Boolean = false

    override fun ownDeletesAreVisible(type: Int): Boolean = false

    override fun ownInsertsAreVisible(type: Int): Boolean = false

    override fun ownUpdatesAreVisible(type: Int): Boolean = false

    override fun storesLowerCaseIdentifiers(): Boolean = false

    override fun storesLowerCaseQuotedIdentifiers(): Boolean = false

    override fun storesMixedCaseIdentifiers(): Boolean = false

    override fun storesMixedCaseQuotedIdentifiers(): Boolean = false

    override fun storesUpperCaseIdentifiers(): Boolean = true

    override fun storesUpperCaseQuotedIdentifiers(): Boolean = true

    override fun supportsAlterTableWithAddColumn(): Boolean = true

    override fun supportsAlterTableWithDropColumn(): Boolean = false

    override fun supportsANSI92EntryLevelSQL(): Boolean = false

    override fun supportsANSI92FullSQL(): Boolean = false

    override fun supportsANSI92IntermediateSQL(): Boolean = false

    override fun supportsBatchUpdates(): Boolean = false

    override fun supportsCatalogsInDataManipulation(): Boolean = false

    override fun supportsCatalogsInIndexDefinitions(): Boolean = false

    override fun supportsCatalogsInPrivilegeDefinitions(): Boolean = false

    override fun supportsCatalogsInProcedureCalls(): Boolean = false

    override fun supportsCatalogsInTableDefinitions(): Boolean = false

    override fun supportsColumnAliasing(): Boolean = true

    override fun supportsConvert(): Boolean = false

    override fun supportsConvert(fromType: Int, toType: Int): Boolean = false

    override fun supportsCoreSQLGrammar(): Boolean = false

    override fun supportsCorrelatedSubqueries(): Boolean = false

    override fun supportsDataDefinitionAndDataManipulationTransactions(): Boolean = false

    override fun supportsDataManipulationTransactionsOnly(): Boolean = false

    override fun supportsDifferentTableCorrelationNames(): Boolean = false

    override fun supportsExpressionsInOrderBy(): Boolean = false

    override fun supportsExtendedSQLGrammar(): Boolean = false

    override fun supportsFullOuterJoins(): Boolean = true

    override fun supportsGetGeneratedKeys(): Boolean = true

    override fun supportsGroupBy(): Boolean = true

    override fun supportsGroupByBeyondSelect(): Boolean = true

    override fun supportsGroupByUnrelated(): Boolean = true

    override fun supportsIntegrityEnhancementFacility(): Boolean = false

    override fun supportsLikeEscapeClause(): Boolean = true

    override fun supportsLimitedOuterJoins(): Boolean = false

    override fun supportsMinimumSQLGrammar(): Boolean = true

    override fun supportsMixedCaseIdentifiers(): Boolean = true

    override fun supportsMixedCaseQuotedIdentifiers(): Boolean = false

    override fun supportsMultipleOpenResults(): Boolean = true

    override fun supportsMultipleResultSets(): Boolean = false

    override fun supportsMultipleTransactions(): Boolean = true

    override fun supportsNamedParameters(): Boolean = true

    override fun supportsNonNullableColumns(): Boolean = true

    override fun supportsOpenCursorsAcrossCommit(): Boolean = true

    override fun supportsOpenCursorsAcrossRollback(): Boolean = true

    override fun supportsOpenStatementsAcrossCommit(): Boolean = true

    override fun supportsOpenStatementsAcrossRollback(): Boolean = true

    override fun supportsOrderByUnrelated(): Boolean = true

    override fun supportsOuterJoins(): Boolean = true

    override fun supportsPositionedDelete(): Boolean = false

    override fun supportsPositionedUpdate(): Boolean = false

    override fun supportsResultSetConcurrency(type: Int, concurrency: Int): Boolean {
        return concurrency == ResultSet.CONCUR_READ_ONLY
    }

    override fun supportsResultSetHoldability(holdability: Int): Boolean {
        when (holdability) {
            ResultSet.HOLD_CURSORS_OVER_COMMIT -> return true
        }
        return false
    }

    override fun supportsResultSetType(type: Int): Boolean {
        when (type) {
            ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.TYPE_FORWARD_ONLY -> return true
        }
        return false
    }

    override fun supportsSavepoints(): Boolean = true

    override fun supportsSchemasInDataManipulation(): Boolean = false

    override fun supportsSchemasInIndexDefinitions(): Boolean = false

    override fun supportsSchemasInPrivilegeDefinitions(): Boolean = false

    override fun supportsSchemasInProcedureCalls(): Boolean = false

    override fun supportsSchemasInTableDefinitions(): Boolean = false

    override fun supportsSelectForUpdate(): Boolean = false

    override fun supportsStatementPooling(): Boolean = false

    override fun supportsStoredProcedures(): Boolean = false

    override fun supportsSubqueriesInComparisons(): Boolean = false

    override fun supportsSubqueriesInExists(): Boolean = true

    override fun supportsSubqueriesInIns(): Boolean = true

    override fun supportsSubqueriesInQuantifieds(): Boolean = false

    override fun supportsTableCorrelationNames(): Boolean = false

    override fun supportsTransactionIsolationLevel(level: Int): Boolean {
        when (level) {
            Connection.TRANSACTION_SERIALIZABLE, Connection.TRANSACTION_READ_COMMITTED, Connection.TRANSACTION_READ_UNCOMMITTED -> return true
        }
        return false
    }

    override fun supportsTransactions(): Boolean = true

    override fun supportsUnion(): Boolean = true

    override fun supportsUnionAll(): Boolean = true

    override fun updatesAreDetected(type: Int): Boolean = false

    override fun usesLocalFilePerTable(): Boolean = false

    override fun usesLocalFiles(): Boolean = true

    override fun autoCommitFailureClosesAllResultSets(): Boolean = false

    override fun getClientInfoProperties(): ResultSet? = null

    override fun getFunctionColumns(catalog: String, schemaPattern: String, functionNamePattern: String, columnNamePattern: String): ResultSet? = null

    override fun getFunctions(catalog: String, schemaPattern: String, functionNamePattern: String): ResultSet? = null

    override fun getRowIdLifetime(): RowIdLifetime = RowIdLifetime.ROWID_UNSUPPORTED

    override fun getSchemas(catalog: String, schemaPattern: String): ResultSet? = null

    override fun supportsStoredFunctionsUsingCallSyntax(): Boolean = false

    override fun <T> unwrap(iface: Class<T>): T? = null

    override fun isWrapperFor(iface: Class<*>): Boolean = false
}
