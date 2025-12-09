package com.loopers.core.infra.database.mysql.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableTransactionManagement
@EntityScan({"com.loopers"})
@EnableJpaRepositories({"com.loopers.core.infra.database.mysql"})
public class JpaConfig {
}
