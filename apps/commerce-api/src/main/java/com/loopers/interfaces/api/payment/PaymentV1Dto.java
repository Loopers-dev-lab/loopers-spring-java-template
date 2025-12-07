package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentInfo;
import com.loopers.application.payment.PaymentPgCardCommand;
import com.loopers.application.payment.PaymentPointCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.time.ZonedDateTime;

public class PaymentV1Dto {

    @Schema(description = "포인트 결제 요청")
    public record PointPaymentRequest(
            @Schema(description = "주문 ID", example = "1")
            @NotNull(message = "주문 ID는 필수입니다.")
            Long orderId,

            @Schema(description = "할인 금액", example = "5000")
            @Min(value = 0, message = "할인 금액은 0원 이상이어야 합니다.")
            Long discountAmount,

            @Schema(description = "쿠폰 ID", example = "1")
            Long couponId,

            @Schema(description = "멱등성 키 (고유한 요청 식별자)",
                    example = "order-1-user-kim123-point-20250104153000")
            @NotBlank(message = "멱등성 키는 필수입니다.")
            @Size(max = 100, message = "멱등성 키는 100자를 초과할 수 없습니다.")
            @Pattern(regexp = "^[-a-zA-Z0-9_:]+$",
                    message = "멱등성 키는 영문, 숫자, -, _, : 만 사용할 수 있습니다.")
            String idempotencyKey
    ) {
        public PaymentPointCommand toCommand(String userId) {
            return PaymentPointCommand.of(
                    userId,
                    orderId,
                    discountAmount,
                    couponId,
                    idempotencyKey
            );
        }
    }

    @Schema(description = "카드 결제 요청")
    public record CardPaymentRequest(
            @Schema(description = "주문 ID", example = "1")
            @NotNull(message = "주문 ID는 필수입니다.")
            Long orderId,

            @Schema(description = "카드 종류", example = "SAMSUNG")
            @NotBlank(message = "카드 종류는 필수입니다.")
            String cardType,

            @Schema(description = "카드 번호", example = "1234-5678-9012-3456")
            @NotBlank(message = "카드 번호는 필수입니다.")
            @Pattern(regexp = "^[0-9-]+$", message = "카드 번호는 숫자와 -만 입력 가능합니다.")
            String cardNo,

            @Schema(description = "할인 금액", example = "5000")
            @Min(value = 0, message = "할인 금액은 0원 이상이어야 합니다.")
            Long discountAmount,

            @Schema(description = "쿠폰 ID", example = "1")
            Long couponId,

            @Schema(description = "멱등성 키 (고유한 요청 식별자)",
                    example = "order-1-user-kim123-card-20250104153000",
                    required = true)
            @NotBlank(message = "멱등성 키는 필수입니다.")
            @Size(max = 100, message = "멱등성 키는 100자를 초과할 수 없습니다.")
            @Pattern(regexp = "^[-a-zA-Z0-9_:]+$",
                    message = "멱등성 키는 영문, 숫자, -, _, : 만 사용할 수 있습니다.")
            String idempotencyKey
    ) {
        public PaymentPgCardCommand toCommand(String userId) {
            return PaymentPgCardCommand.of(
                    userId,
                    orderId,
                    cardType,
                    cardNo,
                    discountAmount,
                    couponId,
                    idempotencyKey
            );
        }
    }

    @Schema(description = "결제 응답")
    public record PaymentResponse(
            @Schema(description = "결제 ID", example = "1")
            Long paymentId,

            @Schema(description = "주문 ID", example = "1")
            Long orderId,

            @Schema(description = "결제 수단", example = "POINT")
            String paymentMethod,

            @Schema(description = "결제 상태", example = "SUCCESS")
            String status,

            @Schema(description = "결제 금액", example = "45000")
            String amount,

            @Schema(description = "PG 거래 ID", example = "20250816:TR:9577c5")
            String transactionId,

            @Schema(description = "멱등성 키", example = "order-1-user-kim123-point-20250104153000")
            String idempotencyKey,

            @Schema(description = "결제 생성 시각", example = "2025-01-04T15:30:00+09:00")
            ZonedDateTime createdAt
    ) {
        public static PaymentResponse from(PaymentInfo info) {
            return new PaymentResponse(
                    info.paymentId(),
                    info.orderId(),
                    info.paymentMethod().name(),
                    info.status().name(),
                    info.amount().toString(),
                    info.transactionId(),
                    info.idempotencyKey(),
                    info.createdAt()
            );
        }
    }

    @Schema(description = "결제 상세 정보")
    public record PaymentDetailResponse(
            @Schema(description = "결제 ID", example = "1")
            Long paymentId,

            @Schema(description = "주문 ID", example = "1")
            Long orderId,

            @Schema(description = "결제 수단", example = "PG_CARD")
            String paymentMethod,

            @Schema(description = "결제 상태", example = "SUCCESS")
            String status,

            @Schema(description = "결제 금액", example = "45000")
            String amount,

            @Schema(description = "카드 타입", example = "SAMSUNG")
            String cardType,

            @Schema(description = "마스킹된 카드 번호", example = "1234-****-****-3456")
            String cardNo,

            @Schema(description = "PG 거래 ID", example = "20250816:TR:9577c5")
            String transactionId,

            @Schema(description = "실패 사유", example = "카드 한도 초과")
            String failureReason,

            @Schema(description = "멱등성 키", example = "order-1-user-kim123-card-20250104153000")
            String idempotencyKey,

            @Schema(description = "결제 생성 시각", example = "2025-01-04T15:30:00+09:00")
            ZonedDateTime createdAt
    ) {
        public static PaymentDetailResponse from(PaymentInfo info) {
            return new PaymentDetailResponse(
                    info.paymentId(),
                    info.orderId(),
                    info.paymentMethod().name(),
                    info.status().name(),
                    info.amount().toString(),
                    info.cardType(),
                    maskCardNo(info.cardNo()),
                    info.transactionId(),
                    info.failureReason(),
                    info.idempotencyKey(),
                    info.createdAt()
            );
        }

        private static String maskCardNo(String cardNo) {
            if (cardNo == null || cardNo.length() < 8) {
                return "****-****-****-****";
            }

            String cleanCardNo = cardNo.replaceAll("-", "");

            if (cleanCardNo.length() < 8) {
                return "****-****-****-****";
            }

            String first4 = cleanCardNo.substring(0, 4);
            String last4 = cleanCardNo.substring(cleanCardNo.length() - 4);

            return first4 + "-****-****-" + last4;
        }
    }
}
