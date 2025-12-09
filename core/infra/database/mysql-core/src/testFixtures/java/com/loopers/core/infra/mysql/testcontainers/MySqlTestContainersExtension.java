package com.loopers.core.infra.mysql.testcontainers;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

public class MySqlTestContainersExtension implements BeforeAllCallback {
    private static final MySQLContainer<?> mySqlContainer;

    static {
        mySqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("loopers")
                .withUsername("test")
                .withPassword("test")
                .withExposedPorts(3306)
                .withCommand(
                        "--character-set-server=utf8mb4",
                        "--collation-server=utf8mb4_general_ci",
                        "--skip-character-set-client-handshake"
                );
        mySqlContainer.start();
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        System.setProperty("datasource.mysql-jpa.main.jdbc-url", mySqlContainer.getJdbcUrl());
        System.setProperty("datasource.mysql-jpa.main.username", mySqlContainer.getUsername());
        System.setProperty("datasource.mysql-jpa.main.password", mySqlContainer.getPassword());
    }
}
