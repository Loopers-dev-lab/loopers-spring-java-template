package com.loopers.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "ranking")
public class RankingConfig {

    private Weight weight = new Weight();
    private int ttlDays = 2;

    @Getter
    @Setter
    public static class Weight {
        private double view = 0.1;
        private double like = 0.2;
        private double order = 0.6;
    }
}
