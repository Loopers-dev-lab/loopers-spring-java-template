package com.loopers.support.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache")
public record CacheProperties(
        HotTargets hotTargets,
        Ttl ttl
) {
    public record HotTargets(
            IdRange brandIdRange,
            IdRange productIdRange,
            Integer hotPageRange
    ) {
        public record IdRange(
                Long min,
                Long max,
                Integer count
        ) {}
    }

    public record Ttl(
            int listSeconds,
            int infoSeconds,
            int statSeconds
    ) {}
}

