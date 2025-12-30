package com.loopers.ranking;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class RankingKeyGenerator {

    private static final String KEY_PREFIX = "ranking:all:";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Duration TTL = Duration.ofDays(2);

    public String resolve(ZonedDateTime dateTime) {
        ZonedDateTime target = dateTime != null ? dateTime : ZonedDateTime.now(ZONE_ID);
        return KEY_PREFIX + FORMATTER.format(target);
    }

    public String resolve(LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now(ZONE_ID);
        return KEY_PREFIX + FORMATTER.format(target);
    }

    public String resolve(String date) {
        LocalDate target = parseDate(date);
        return KEY_PREFIX + FORMATTER.format(target);
    }

    public LocalDate parseDate(String date) {
        if (!StringUtils.hasText(date)) {
            return LocalDate.now(ZONE_ID);
        }
        return LocalDate.parse(date, FORMATTER);
    }

    public Duration ttl() {
        return TTL;
    }
}
