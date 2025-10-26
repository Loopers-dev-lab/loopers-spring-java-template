package com.loopers.core.infra.database.mysql.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
class DataSourceConfig {

    @Bean
    @ConfigurationProperties(prefix = "datasource.mysql-jpa.main")
    HikariConfig mySqlMainHikariConfig() {
        return new HikariConfig();
    }

    @Primary
    @Bean
    HikariDataSource mySqlMainDataSource(@Qualifier("mySqlMainHikariConfig") HikariConfig hikariConfig) {
        return new HikariDataSource(hikariConfig);
    }
}
