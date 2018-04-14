package io.requery.example.springboot;

import io.requery.Persistable;
import io.requery.cache.WeakEntityCache;
import io.requery.example.springboot.entity.Models;
import io.requery.meta.EntityModel;
import io.requery.sql.*;
import io.requery.sql.platform.H2;
import io.requery.sql.platform.SQLite;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.Executors;

@SpringBootApplication
public class ExampleSpringbootApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleSpringbootApplication.class, args);
    }


}
