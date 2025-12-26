package com.loopers.infrastructure.ranking;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(RankingRedisProperties.class)
public class RankingConfig {}
