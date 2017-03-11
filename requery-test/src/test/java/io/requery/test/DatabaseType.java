package io.requery.test;

import com.mysql.cj.jdbc.MysqlDataSource;
import io.requery.sql.Platform;
import io.requery.sql.platform.Derby;
import io.requery.sql.platform.MySQL;
import io.requery.sql.platform.Oracle;
import io.requery.sql.platform.PostgresSQL;
import io.requery.sql.platform.SQLServer;
import io.requery.sql.platform.SQLite;
import org.apache.derby.jdbc.EmbeddedDataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.hsqldb.jdbc.JDBCDataSource;
import org.postgresql.ds.PGSimpleDataSource;
import org.sqlite.SQLiteConfig;
import org.sqlite.SQLiteDataSource;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Locale;
import java.util.Properties;

/**
 * Enumeration of the database types being tested. Each database type is configured through a
 * properties file located in resources/io.requery.test. Example for oracle, the file must be called
 * oracle.properties and contain:
 *
 * <pre>
 * server=someserver.local
 * port=1521
 * database=ORCL
 * user=sa
 * password=secret
 * </pre>
 *
 * @author Nikhil Purushe
 */
public enum DatabaseType {

    POSTGRES(PostgresSQL.class, PGSimpleDataSource.class),
    MYSQL(MySQL.class, MysqlDataSource.class),
    SQLSERVER(SQLServer.class, "com.microsoft.sqlserver.jdbc.SQLServerDataSource"),
    ORACLE(Oracle.class, "oracle.jdbc.pool.OracleDataSource") {
        @Override
        CommonDataSource createDataSource() {
            CommonDataSource dataSource = super.createDataSource();
            setProperty(dataSource, "setDriverType", "thin");
            return dataSource;
        }
    },
    H2(io.requery.sql.platform.H2.class, JdbcDataSource.class) {
        @Override
        DataSource createDataSource() {
            JdbcDataSource dataSource = new JdbcDataSource();
            dataSource.setUrl("jdbc:h2:~/testh2");
            dataSource.setUser("sa");
            dataSource.setPassword("sa");
            return dataSource;
        }
    },
    HSQL(io.requery.sql.platform.HSQL.class, JDBCDataSource.class) {
        @Override
        DataSource createDataSource() {
            JDBCDataSource dataSource = new JDBCDataSource();
            dataSource.setUrl("jdbc:hsqldb:hsqltest");
            dataSource.setUser("sa");
            dataSource.setPassword("sa");
            return dataSource;
        }
    },
    SQLITE(SQLite.class, SQLiteDataSource.class) {
        @Override
        DataSource createDataSource() {
            SQLiteDataSource dataSource = new SQLiteDataSource();
            dataSource.setUrl("jdbc:sqlite:/tmp/test.sqlite");
            SQLiteConfig config = new SQLiteConfig();
            config.setDateClass("TEXT");
            dataSource.setConfig(config);
            try (Statement statement = dataSource.getConnection().createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            return dataSource;
        }
    },
    DERBY(Derby.class, EmbeddedDataSource.class) {
        @Override
        DataSource createDataSource() {
            EmbeddedDataSource dataSource = new EmbeddedDataSource();
            dataSource.setUser("");
            dataSource.setPassword("");
            dataSource.setDatabaseName("dbtest");
            dataSource.setCreateDatabase("create");
            dataSource.setConnectionAttributes("upgrade=true");
            return dataSource;
        }
    };

    public static CommonDataSource getDataSource(Platform platform) {
        for(DatabaseType type : values()) {
            if(type.matches(platform)) {
                return type.createDataSource();
            }
        }
        throw new UnsupportedOperationException();
    }

    private final Class<? extends Platform> platformClass;
    private Class<? extends CommonDataSource> dataSourceClass;
    private String dataSourceClassName;

    DatabaseType(Class<? extends Platform> platformClass,
                 Class<? extends DataSource> dataSourceClass) {
        this.dataSourceClass = dataSourceClass;
        this.platformClass = platformClass;
    }

    DatabaseType(Class<? extends Platform> platformClass,
                 String dataSourceClassName) {
        this.platformClass = platformClass;
        this.dataSourceClass = null;
        this.dataSourceClassName = dataSourceClassName;
    }

    boolean matches(Platform platform) {
        return platform.getClass().isAssignableFrom(platformClass);
    }

    CommonDataSource createDataSource() {
        try {
            if (dataSourceClass == null) {
                dataSourceClass = Class.forName(dataSourceClassName)
                    .asSubclass(CommonDataSource.class);
            }
            CommonDataSource dataSource = dataSourceClass.newInstance();
            String fileName = platformClass.getSimpleName().toLowerCase(Locale.US);
            Properties properties = new Properties();
            String localPath = "src/test/resources/io/requery/test/local/";
            String ciPath = "src/test/resources/io/requery/test/ci/";
            File[] files = new File[] {
                new File(localPath + fileName + ".properties"),
                new File(ciPath + fileName + ".properties")};
            for (File file : files) {
                if (file.exists()) {
                    try (FileInputStream stream = new FileInputStream(file)) {
                        properties.load(stream);
                    }
                    break;
                }
            }

            String server = properties.getProperty("server");
            Integer port = Integer.parseInt(properties.getProperty("port"));
            String user = properties.getProperty("user");
            String password = properties.getProperty("password");
            String database = properties.getProperty("database");

            setProperty(dataSource, "setServerName", server);
            setProperty(dataSource, "setPortNumber", port);
            setProperty(dataSource, "setDatabaseName", database);
            setProperty(dataSource, "setUser", user);
            setProperty(dataSource, "setPassword", password);

            return dataSource;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    void setProperty(CommonDataSource dataSource, String method, Object value) {
        try {
            Class<?> parameterClass = value.getClass();
            if (parameterClass == Integer.class) {
                parameterClass = int.class;
            }
            dataSource.getClass().getMethod(method, parameterClass).invoke(dataSource, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
