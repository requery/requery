package io.requery.example.springboot;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.requery.Persistable;
import io.requery.cache.WeakEntityCache;
import io.requery.example.springboot.entity.Models;
import io.requery.jackson.EntityMapper;
import io.requery.sql.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executors;

@Configuration
public class DatabaseConfiguration {

    @Bean
    public ObjectMapper objectMapper(@Autowired EntityDataStore entityDataStore) {
        return new EntityMapper(Models.DEFAULT, entityDataStore);
    }

    @Bean
    public EntityDataStore<Persistable> provideDataStore() {
        ConnectionProvider connectionProvider = new ConnectionProvider() {
            @Override
            public Connection getConnection() throws SQLException {
                return DriverManager.getConnection("jdbc:h2:~/test", "sa", "");
            }
        };
        io.requery.sql.Configuration configuration = new ConfigurationBuilder(connectionProvider, Models.DEFAULT)
                .useDefaultLogging()
                .setEntityCache(new WeakEntityCache())
                .setWriteExecutor(Executors.newSingleThreadExecutor())
                .build();

        SchemaModifier tables = new SchemaModifier(configuration);
        tables.createTables(TableCreationMode.DROP_CREATE);
        return new EntityDataStore<>(configuration);
    }
}