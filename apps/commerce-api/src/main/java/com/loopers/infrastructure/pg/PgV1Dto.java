package com.loopers.infrastructure.pg;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.CollectionUtils;

import java.util.List;

public class PgV1Dto {
    public record PgPaymentRequest(
            String orderId,
            String cardType,
            String cardNo,
            Long amount,
            String callbackUrl
    ) {
    }

    public record PgTransactionResponse(
            String transactionKey,
            String status,
            String reason
    ) {
    }

    public record PgTransactionDetailResponse(
            String transactionKey,
            String orderId,
            String cardType,
            String cardNo,
            Long amount,
            String status,
            String pgPaymentReason
    ) {
    }

    public record PgOrderResponse(
            String orderId,
            List<PgTransactionResponse> transactions,
            Boolean isFallback
    ) {
        public PgOrderResponse {
            if (StringUtils.isBlank(orderId)) {
                throw new CoreException(ErrorType.BAD_REQUEST, "orderId가 비어있습니다.");
            }
            // isFallback이 true인 경우(서킷브레이커 fallback)에는 transactions가 비어있어도 허용
            if (BooleanUtils.isNotTrue(isFallback) && CollectionUtils.isEmpty(transactions)) {
                throw new CoreException(ErrorType.BAD_REQUEST, "transactions가 비어있습니다.");
            }
        }
    }
}
