package com.loopers.config.ranking;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ranking")
public record RankingProperties(
    Weights weights,
    int ttlDays,
    CarryOver carryOver
) {
    public record Weights(
        double order,
        double like,
        double view
    ) {}
    
    public record CarryOver(
        boolean enabled,
        double weight,
        String schedule
    ) {}
}



