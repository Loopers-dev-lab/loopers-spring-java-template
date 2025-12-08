package com.loopers.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

/**
 * @author hyunjikoh
 * @since 2025. 12. 4.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MaskingUtil {
    public static String maskCardNumber(String cardNo) {
        if (cardNo == null || cardNo.length() < 4)
            return "****";
        String digits = cardNo.replaceAll("[^0-9]", "");
        if (digits.length() < 8)
            return "****";
        return digits.substring(0, 4) + "-****-****-" + digits.substring(digits.length() - 4);
    }
}
