package com.loopers.domain.payment;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 결제 도메인 모델
 * - PG 시스템과의 결제 정보를 관리
 * - 주문(Order)과 1:N 관계 (하나의 주문에 여러 결제 시도 가능)
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Payment {

    private Long id;
    private String userId;
    private Long orderId;                    // Order ID (주문과 연결)
    private String pgTransactionKey;         // PG 트랜잭션 키
    private CardType cardType;
    private String maskedCardNo;             // 마스킹된 카드 번호 (보안)
    private BigDecimal amount;
    private PaymentStatus status;
    private String failureReason;            // 실패 사유
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    /**
     * 결제 생성 팩토리 메서드 (PG 요청 전)
     */
    public static Payment create(
            String userId,
            Long orderId,
            CardType cardType,
            String cardNo,
            BigDecimal amount
    ) {
        validateUserId(userId);
        validateOrderId(orderId);
        validateCardType(cardType);
        validateCardNo(cardNo);
        validateAmount(amount);

        LocalDateTime now = LocalDateTime.now();

        return new Payment(
                null,
                userId,
                orderId,
                null,  // PG 트랜잭션 키는 PG 응답 후 설정
                cardType,
                maskCardNo(cardNo),
                amount,
                PaymentStatus.PENDING,
                null,
                now,
                now
        );
    }

    /**
     * 재구성 팩토리 메서드 (Infrastructure에서 사용)
     */
    public static Payment reconstruct(
            Long id,
            String userId,
            Long orderId,
            String pgTransactionKey,
            CardType cardType,
            String maskedCardNo,
            BigDecimal amount,
            PaymentStatus status,
            String failureReason,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt
    ) {
        return new Payment(
                id,
                userId,
                orderId,
                pgTransactionKey,
                cardType,
                maskedCardNo,
                amount,
                status,
                failureReason,
                createdAt,
                modifiedAt
        );
    }

    /**
     * PG 트랜잭션 키 설정 (PG 요청 성공 후)
     */
    public void assignPgTransactionKey(String pgTransactionKey) {
        if (this.pgTransactionKey != null) {
            throw new CoreException(ErrorType.CONFLICT, "이미 PG 트랜잭션 키가 설정되어 있습니다");
        }
        if (pgTransactionKey == null || pgTransactionKey.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "PG 트랜잭션 키는 필수입니다");
        }

        this.pgTransactionKey = pgTransactionKey;
        this.modifiedAt = LocalDateTime.now();
    }

    /**
     * 결제 성공 처리
     */
    public void success() {
        if (this.status == PaymentStatus.SUCCESS) {
            return; // 이미 성공 (멱등성)
        }
        if (this.status == PaymentStatus.FAILED) {
            throw new CoreException(ErrorType.CONFLICT, "이미 실패한 결제는 성공으로 변경할 수 없습니다");
        }

        this.status = PaymentStatus.SUCCESS;
        this.failureReason = null;
        this.modifiedAt = LocalDateTime.now();
    }

    /**
     * 결제 실패 처리
     */
    public void fail(String reason) {
        if (this.status == PaymentStatus.SUCCESS) {
            throw new CoreException(ErrorType.CONFLICT, "이미 성공한 결제는 실패로 변경할 수 없습니다");
        }
        if (this.status == PaymentStatus.FAILED) {
            return; // 이미 실패 (멱등성)
        }

        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        this.modifiedAt = LocalDateTime.now();
    }

    /**
     * 결제 성공 여부
     */
    public boolean isSuccess() {
        return this.status == PaymentStatus.SUCCESS;
    }

    /**
     * 결제 실패 여부
     */
    public boolean isFailed() {
        return this.status == PaymentStatus.FAILED;
    }

    /**
     * 결제 대기 여부
     */
    public boolean isPending() {
        return this.status == PaymentStatus.PENDING;
    }

    /**
     * 결제 소유자 확인
     */
    public boolean isOwnedBy(String userId) {
        return this.userId.equals(userId);
    }

    /**
     * 카드 번호 마스킹 (보안)
     * 예: 1234-5678-9012-3456 → 1234-****-****-3456
     */
    private static String maskCardNo(String cardNo) {
        if (cardNo == null || cardNo.length() < 19) {
            throw new CoreException(ErrorType.BAD_REQUEST, "잘못된 카드 번호 형식입니다");
        }

        // xxxx-xxxx-xxxx-xxxx 형식에서 중간 8자리 마스킹
        String[] parts = cardNo.split("-");
        if (parts.length != 4) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드 번호는 xxxx-xxxx-xxxx-xxxx 형식이어야 합니다");
        }

        return parts[0] + "-****-****-" + parts[3];
    }

    // === Validation Methods ===

    private static void validateUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID는 필수입니다");
        }
    }

    private static void validateOrderId(Long orderId) {
        if (orderId == null || orderId <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "주문 ID는 양수여야 합니다");
        }
    }

    private static void validateCardType(CardType cardType) {
        if (cardType == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드 타입은 필수입니다");
        }
    }

    private static void validateCardNo(String cardNo) {
        if (cardNo == null || !cardNo.matches("^\\d{4}-\\d{4}-\\d{4}-\\d{4}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "카드 번호는 xxxx-xxxx-xxxx-xxxx 형식이어야 합니다");
        }
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "결제 금액은 0보다 커야 합니다");
        }
    }
}
