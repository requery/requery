package io.requery.test;

import io.requery.meta.EntityModel;
import io.requery.query.Result;
import io.requery.sql.EntityDataStore;
import io.requery.sql.SchemaModifier;
import io.requery.sql.TableCreationMode;
import io.requery.sql.platform.H2;
import io.requery.sql.Platform;
import io.requery.test.model.Models;
import io.requery.test.model.Person;
import org.junit.Test;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import javax.sql.DataSource;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Performs a query using raw JDBC and a query using the library and compares the execution times.
 * TODO extend to support inserts and more queries
 */
@State(Scope.Thread)
public class BenchmarkTest {

    private Platform platform;
    private DataSource dataSource;
    private EntityDataStore<Object> data;

    public BenchmarkTest() {
        this.platform = new H2();
    }

    @Test
    public void testCompareQuery() throws SQLException, RunnerException {
        Options options = new OptionsBuilder()
            .include(getClass().getName() + ".*")
            .mode(Mode.SingleShotTime)
            .timeUnit(TimeUnit.MILLISECONDS)
            .warmupTime(TimeValue.seconds(5))
            .warmupIterations(0)
            .measurementTime(TimeValue.seconds(10))
            .measurementIterations(5)
            .threads(1)
            .forks(2)
            .build();
        new Runner(options).run();
    }

    @Setup
    public void setup() {
        EntityModel model = Models.DEFAULT;
        dataSource = (DataSource) DatabaseType.getDataSource(platform);
        data = new EntityDataStore<>(dataSource, model);
        new SchemaModifier(dataSource, model).createTables(TableCreationMode.DROP_CREATE);
        final int count = 10000;
        data.runInTransaction(new Callable<Object>() {
            @Override
            public Object call() throws Exception {
                for (int i = 0; i < count; i++) {
                    Person person = FunctionalTest.randomPerson();
                    data.insert(person);
                }
                return null;
            }
        });
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void queryPerson() {
        try (Result<Person> results = data.select(Person.class).limit(10000).get()) {
            for (Person p : results) {
                p.getName();
            }
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.SingleShotTime)
    public void queryJdbc() throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id , name , email , birthday," +
                             " age, homepage, uuid FROM Person LIMIT 10000 ")) {

            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    resultSet.getLong(1); //id
                    String name = resultSet.getString(2);
                    String email = resultSet.getString(3);
                    Date birthday = resultSet.getDate(4);
                    Integer age = resultSet.getInt(5);
                    String home = resultSet.getString(6);
                    byte[] uuid = resultSet.getBytes(7);

                    Person p = new Person();
                    p.setName(name);
                    p.setEmail(email);
                    p.setUUID(uuid == null ? null : UUID.nameUUIDFromBytes(uuid));
                    p.setBirthday(birthday);
                    p.setHomepage(home == null ? null : new URL(home));
                    p.setAge(age);
                }
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException();
        }
    }
}
