package com.loopers.support.test;

import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public abstract class IntegrationTestSupport {

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }
}
