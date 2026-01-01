package com.loopers;

import com.loopers.config.BatchJobProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties(BatchJobProperties.class)
@SpringBootApplication
public class CommerceBatchApplication {

  public static void main(String[] args) {
    SpringApplication.run(CommerceBatchApplication.class, args);
  }
}
