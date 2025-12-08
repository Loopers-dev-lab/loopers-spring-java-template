package com.loopers.domain.payment;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@NoArgsConstructor
public class PaymentKeyGenerator {
    private static final String PREFIX_CARD = "PG_";
    private static final String PREFIX_POINT = "PO_";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String generateKeyForPoint() {
        LocalDateTime now = LocalDateTime.now();
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return PREFIX_POINT + now.format(FORMATTER) + uuid;
    }

    public static String generateKeyForCard() {
        LocalDateTime now = LocalDateTime.now();
        String uuid = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return PREFIX_CARD + now.format(FORMATTER) + uuid;
    }
}
