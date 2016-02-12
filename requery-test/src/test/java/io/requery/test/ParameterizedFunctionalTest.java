package io.requery.test;

import io.requery.cache.EntityCacheBuilder;
import io.requery.meta.EntityModel;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.EntityDataStore;
import io.requery.sql.SchemaModifier;
import io.requery.sql.TableCreationMode;
import io.requery.sql.platform.Derby;
import io.requery.sql.platform.H2;
import io.requery.sql.platform.HSQL;
import io.requery.sql.platform.MySQL;
import io.requery.sql.platform.Oracle;
import io.requery.sql.Platform;
import io.requery.sql.platform.PostgresSQL;
import io.requery.sql.platform.SQLServer;
import io.requery.sql.platform.SQLite;
import io.requery.test.model.Models;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.spi.CachingProvider;
import javax.sql.CommonDataSource;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;

/**
 * Runs the functional tests against several different databases.
 *
 * @author Nikhil Purushe
 */
@RunWith(Parameterized.class)
public class ParameterizedFunctionalTest extends FunctionalTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Platform> data() {
        return Arrays.<Platform>asList(
            new Oracle(),
            new SQLServer(),
            new MySQL(),
            new PostgresSQL(),
            new Derby(),
            new SQLite(),
            new H2(),
            new HSQL());
    }

    private Platform platform;

    public ParameterizedFunctionalTest(Platform platform) {
        this.platform = platform;
    }

    @Before
    public void setup() throws SQLException {
        CommonDataSource dataSource = DatabaseType.getDataSource(platform);
        EntityModel model = Models.DEFAULT;

        CachingProvider provider = Caching.getCachingProvider();
        CacheManager cacheManager = provider.getCacheManager();
        Configuration configuration = new ConfigurationBuilder(dataSource, model)
            .useDefaultLogging()
            // work around bug reusing prepared statements in xerial sqlite
            .setStatementCacheSize(platform instanceof SQLite ? 0 : 10)
            .setBatchUpdateSize(50)
            .setEntityCache(new EntityCacheBuilder(model)
                .useReferenceCache(true)
                .useSerializableCache(true)
                .useCacheManager(cacheManager)
                .build())
            .build();
        data = new EntityDataStore<>(configuration);
        SchemaModifier tables = new SchemaModifier(configuration);
        try {
            tables.dropTables();
        } catch (Exception e) {
            // expected if 'drop if exists' not supported (so ignore in that case)
            if (!platform.supportsIfExists()) {
                throw e;
            }
        }
        TableCreationMode mode = TableCreationMode.CREATE;
        System.out.println(tables.createTablesString(mode));
        tables.createTables(mode);
    }
}
