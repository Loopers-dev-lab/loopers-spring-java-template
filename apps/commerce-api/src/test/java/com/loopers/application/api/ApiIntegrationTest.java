package com.loopers.application.api;

import com.loopers.core.infra.mysql.testcontainers.MySqlTestContainersExtension;
import com.loopers.core.infra.mysql.util.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(MySqlTestContainersExtension.class)
public class ApiIntegrationTest {

    @Autowired
    protected TestRestTemplate testRestTemplate;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void databaseCleanUp() {
        databaseCleanUp.truncateAllTables();
    }
}
