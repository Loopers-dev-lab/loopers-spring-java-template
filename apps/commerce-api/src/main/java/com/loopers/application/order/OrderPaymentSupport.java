package com.loopers.application.order;

import com.loopers.domain.order.OrderStatus;
import com.loopers.domain.payment.PaymentStatus;
import com.loopers.infrastructure.payment.dto.PgPaymentV1Dto.Response;
import com.loopers.infrastructure.payment.dto.PgPaymentV1Dto.TransactionStatus;
import com.loopers.interfaces.api.ApiResponse;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import static com.loopers.infrastructure.payment.dto.PgPaymentV1Dto.TransactionStatus.SUCCESS;

public final class OrderPaymentSupport {

    private OrderPaymentSupport() {
    }

    public static OrderStatus mapOrderStatus(TransactionStatus status) {
        if (status == SUCCESS) {
            return OrderStatus.COMPLETE;
        }
        if (status == TransactionStatus.FAILED) {
            return OrderStatus.FAIL;
        }
        return OrderStatus.PENDING;
    }

    public static PaymentStatus mapPaymentStatus(TransactionStatus status) {
        if (status == SUCCESS) {
            return PaymentStatus.SUCCESS;
        }
        if (status == TransactionStatus.FAILED) {
            return PaymentStatus.FAIL;
        }
        return PaymentStatus.PENDING;
    }

    public static Response requirePgResponse(ApiResponse<Response> response) {
        Response data = response != null ? response.data() : null;
        if (data == null) {
            throw new CoreException(ErrorType.INTERNAL_ERROR, "PG 응답을 확인할 수 없습니다.");
        }
        return data;
    }
}
