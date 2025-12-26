package com.loopers.config.ranking;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ranking")
public record RankingProperties(
    Weights weights,
    int ttlDays
) {
    public record Weights(
        double order,
        double like,
        double view
    ) {}
}



