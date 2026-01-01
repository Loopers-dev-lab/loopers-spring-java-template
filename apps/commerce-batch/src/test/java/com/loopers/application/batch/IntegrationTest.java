package com.loopers.application.batch;

import com.loopers.core.infra.mysql.testcontainers.MySqlTestContainersExtension;
import com.loopers.core.infra.mysql.util.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@ExtendWith(MySqlTestContainersExtension.class)
public class IntegrationTest {

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void cleanup() {
        databaseCleanUp.truncateAllTables();
    }
}
