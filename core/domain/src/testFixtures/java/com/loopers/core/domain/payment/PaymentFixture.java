package com.loopers.core.domain.payment;

import com.loopers.core.domain.order.vo.OrderKey;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.CardNo;
import com.loopers.core.domain.payment.vo.CardType;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.payment.vo.PaymentId;
import com.loopers.core.domain.user.vo.UserId;
import org.instancio.Instancio;

import java.math.BigDecimal;

import static org.instancio.Select.field;

public class PaymentFixture {

    public static Payment create() {
        return Instancio.of(Payment.class)
                .set(field(Payment::getId), PaymentId.empty())
                .set(field(Payment::getOrderKey), OrderKey.generate())
                .set(field(Payment::getUserId), Instancio.create(UserId.class))
                .set(field(Payment::getCardType), new CardType("CREDIT"))
                .set(field(Payment::getCardNo), new CardNo("1234567890123456"))
                .set(field(Payment::getAmount), new PayAmount(new BigDecimal(10000)))
                .set(field(Payment::getStatus), PaymentStatus.PENDING)
                .create();
    }

    public static Payment createWith(OrderKey orderKey, UserId userId) {
        return Instancio.of(Payment.class)
                .set(field(Payment::getId), PaymentId.empty())
                .set(field(Payment::getOrderKey), orderKey)
                .set(field(Payment::getUserId), userId)
                .set(field(Payment::getCardType), new CardType("CREDIT"))
                .set(field(Payment::getCardNo), new CardNo("1234567890123456"))
                .set(field(Payment::getAmount), new PayAmount(new BigDecimal(10000)))
                .set(field(Payment::getStatus), PaymentStatus.PENDING)
                .create();
    }
}
