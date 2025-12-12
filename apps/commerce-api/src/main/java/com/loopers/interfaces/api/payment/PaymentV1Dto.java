package com.loopers.interfaces.api.payment;

import com.loopers.application.payment.PaymentInfo;
import com.loopers.domain.payment.CardType;
import com.loopers.domain.payment.PaymentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Payment V1 API DTO
 */
public class PaymentV1Dto {

    /**
     * 결제 요청 DTO
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "결제 요청")
    public static class PaymentRequest {
        @Schema(description = "주문 ID", example = "1")
        private Long orderId;

        @Schema(description = "카드 타입", example = "SAMSUNG")
        private CardType cardType;

        @Schema(description = "카드 번호 (xxxx-xxxx-xxxx-xxxx)", example = "1234-5678-9012-3456")
        private String cardNo;

        @Schema(description = "결제 금액", example = "10000")
        private BigDecimal amount;

        @Schema(description = "콜백 URL", example = "http://localhost:8080/api/v1/payments/callback")
        private String callbackUrl;
    }

    /**
     * 결제 응답 DTO
     */
    @Getter
    @AllArgsConstructor
    @Schema(description = "결제 응답")
    public static class PaymentResponse {
        @Schema(description = "결제 ID", example = "1")
        private Long id;

        @Schema(description = "주문 ID", example = "1")
        private Long orderId;

        @Schema(description = "PG 트랜잭션 키", example = "20250103:TR:abc123")
        private String pgTransactionKey;

        @Schema(description = "카드 타입", example = "SAMSUNG")
        private CardType cardType;

        @Schema(description = "마스킹된 카드 번호", example = "1234-****-****-3456")
        private String maskedCardNo;

        @Schema(description = "결제 금액", example = "10000")
        private BigDecimal amount;

        @Schema(description = "결제 상태", example = "PENDING")
        private PaymentStatus status;

        @Schema(description = "실패 사유")
        private String failureReason;

        @Schema(description = "생성 시간", example = "2025-01-03T10:00:00")
        private LocalDateTime createdAt;

        public static PaymentResponse from(PaymentInfo info) {
            return new PaymentResponse(
                    info.getId(),
                    info.getOrderId(),
                    info.getPgTransactionKey(),
                    info.getCardType(),
                    info.getMaskedCardNo(),
                    info.getAmount(),
                    info.getStatus(),
                    info.getFailureReason(),
                    info.getCreatedAt()
            );
        }
    }

    /**
     * 콜백 요청 DTO (PG로부터 수신)
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "PG 콜백 요청")
    public static class CallbackRequest {
        @Schema(description = "PG 트랜잭션 키", example = "20250103:TR:abc123")
        private String transactionKey;

        @Schema(description = "결제 상태", example = "SUCCESS")
        private String status;

        @Schema(description = "사유/메시지", example = "정상 승인되었습니다")
        private String reason;
    }
}
