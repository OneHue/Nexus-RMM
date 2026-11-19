package com.nexus.server.health;

import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

// @Component tells spring boot to automatically create and manage an instance of the class
@Component
public class DatabaseConnectionCheck implements CommandLineRunner {
    // CommandLineRunner allows running code on Spring Boot Application start.

    // JdbcTemplate is a Spring Boot utility that simplifies executing SQL queries.
    private final JdbcTemplate jdbcTemplate;

    public DatabaseConnectionCheck(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // Runs when the Spring Boot Application starts
    @Override
    public void run(String... args) {
        String database = jdbcTemplate.queryForObject(
          "SELECT current_database()",
          String.class
        );

        String user = jdbcTemplate.queryForObject(
          "SELECT current_user",
          String.class
        );

        System.out.println(
                "Nexus connected to PostgreSQL: "
                + database + " (user: " + user + ")"
        );
    }
}
