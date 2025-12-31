package com.loopers.application.ranking;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.Duration;
import com.loopers.config.ranking.RankingProperties;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingAggregator {

    private final RedisTemplate<String, String> redisTemplate;
    private final RankingProperties rankingProperties;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final double VIEW_WEIGHT = 0.1d;
    private static final double LIKE_WEIGHT = 0.2d;
    private static final double ORDER_WEIGHT = 0.6d;

    /**
     * ZSET에 누적한다.
     */
    public void aggregate(List<Map<String, Object>> events) {
        if (events == null || events.isEmpty()) {
            return;
        }
        Map<String, Map<String, Double>> increments = new HashMap<>();

        for (Map<String, Object> event : events) {
            String eventType = asString(event.get("eventType"));
            Map<String, Object> payload = castToMap(event.get("payload"));
            if (eventType == null || payload == null) {
                continue;
            }
            String dayKey = resolveDayKey(asString(event.get("occurredAt")));
            String zsetKey = rankingKey(dayKey);
            String productId = asString(payload.get("productId"));
            if (productId == null) continue;

            double delta = 0.0d;
            switch (eventType) {
                case "PRODUCT_VIEWED" -> delta = VIEW_WEIGHT * 1.0d;
                case "LIKE_CREATED" -> delta = LIKE_WEIGHT * 1.0d;
                case "LIKE_DELETED" -> delta = -LIKE_WEIGHT * 1.0d;
                case "ORDER_ITEM_METRIC" -> {
                    double amount = extractAmount(payload);
                    if (amount <= 0.0d) {
                        Integer quantity = asInt(payload.get("quantity"));
                        amount = quantity == null ? 0.0d : Math.max(0, quantity);
                    }
                    delta = ORDER_WEIGHT * amount;
                }
                default -> delta = 0.0d;
            }
            if (delta == 0.0d) continue;
            increments.computeIfAbsent(zsetKey, k -> new HashMap<>())
                    .merge(productId, delta, Double::sum);
        }

        if (increments.isEmpty()) return;

        redisTemplate.executePipelined(new SessionCallback<Object>() {
            @Override
            public Object execute(RedisOperations operations) {
                @SuppressWarnings("unchecked")
                RedisOperations<String, String> ops = (RedisOperations<String, String>) operations;
                for (Map.Entry<String, Map<String, Double>> entry : increments.entrySet()) {
                    String zsetKey = entry.getKey();
                    for (Map.Entry<String, Double> memberDelta : entry.getValue().entrySet()) {
                        String member = memberDelta.getKey();
                        Double delta = memberDelta.getValue();
                        ops.opsForZSet().incrementScore(zsetKey, member, delta);
                    }
                }
                return null;
            }
        });

        // TTL 설정
        Duration ttl = Duration.ofDays(Math.max(1, rankingProperties.getTtlDays()));
        for (String zsetKey : increments.keySet()) {
            redisTemplate.expire(zsetKey, ttl);
        }

        log.info("랭킹 집계 완료 - keys={}, totalOps={}",
                increments.keySet(),
                increments.values().stream().mapToInt(m -> m.size()).sum());
    }

    private static String asString(Object v) {
        return v == null ? null : v.toString();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castToMap(Object v) {
        if (v instanceof Map<?, ?> map) {
            return (Map<String, Object>) map;
        }
        return null;
    }

    private static String resolveDayKey(String occurredAt) {
        try {
            if (occurredAt != null) {
                LocalDateTime ldt = LocalDateTime.parse(occurredAt);
                return ldt.toLocalDate().format(DAY_FMT);
            }
        } catch (Exception ignored) {}
        return LocalDate.now().format(DAY_FMT);
    }

    private String rankingKey(String dayKey) {
        String prefix = rankingProperties.getPrefix();
        if (prefix == null || prefix.isBlank()) {
            prefix = "ranking:all";
        }
        return prefix + ":" + dayKey;
    }

    private static double extractAmount(Map<String, Object> payload) {
        Object amt = payload.get("totalAmount");
        if (amt == null) return 0.0d;
        try {
            return new java.math.BigDecimal(amt.toString()).doubleValue();
        } catch (Exception e) {
            return 0.0d;
        }
    }

    private static Integer asInt(Object v) {
        try {
            return v == null ? null : Integer.valueOf(v.toString());
        } catch (Exception e) {
            return null;
        }
    }
}


