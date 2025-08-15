package com.loopers.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.List;

@Configuration
@EnableCaching
public class CacheConfig {
    @Bean
    public CacheManager caffeineCacheManager() {
        var detail = new CaffeineCache("productDetail",
                Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(30))
                        .maximumSize(100_000).recordStats().build());

        var list = new CaffeineCache("productList",
                Caffeine.newBuilder().expireAfterWrite(Duration.ofMinutes(5))
                        .maximumSize(100_000).recordStats().build());

        var manager = new SimpleCacheManager();
        manager.setCaches(List.of(detail, list));
        return manager;
    }
}
