package com.loopers.core.domain.payment;

import com.loopers.core.domain.payment.type.PgPaymentStatus;
import com.loopers.core.domain.payment.vo.PaymentId;
import com.loopers.core.domain.payment.vo.PgPaymentId;
import com.loopers.core.domain.payment.vo.TransactionKey;
import org.instancio.Instancio;

import static org.instancio.Select.field;

public class PgPaymentFixture {

    public static PgPayment create() {
        return Instancio.of(PgPayment.class)
                .set(field(PgPayment::getId), PgPaymentId.empty())
                .set(field(PgPayment::getPaymentId), Instancio.create(PaymentId.class))
                .set(field(PgPayment::getTransactionKey), Instancio.create(TransactionKey.class))
                .set(field(PgPayment::getStatus), PgPaymentStatus.PENDING)
                .create();
    }

    public static PgPayment createWith(PaymentId paymentId) {
        return Instancio.of(PgPayment.class)
                .set(field(PgPayment::getId), PgPaymentId.empty())
                .set(field(PgPayment::getPaymentId), paymentId)
                .set(field(PgPayment::getStatus), PgPaymentStatus.PENDING)
                .create();
    }

    public static PgPayment createWith(PaymentId paymentId, TransactionKey transactionKey) {
        return Instancio.of(PgPayment.class)
                .set(field(PgPayment::getId), PgPaymentId.empty())
                .set(field(PgPayment::getPaymentId), paymentId)
                .set(field(PgPayment::getTransactionKey), transactionKey)
                .set(field(PgPayment::getStatus), PgPaymentStatus.PENDING)
                .create();
    }
}
