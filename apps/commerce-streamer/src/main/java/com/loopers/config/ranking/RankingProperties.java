package com.loopers.config.ranking;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "ranking")
public class RankingProperties {
    /**
     * 랭킹 ZSET 키 프리픽스. 예: ranking:all
     */
    private String prefix = "ranking:all";

    /**
     * 랭킹 키 보존 일수 (TTL)
     */
    private int ttlDays = 7;
}



