package com.loopers.core.domain.payment;

import com.loopers.core.domain.order.vo.OrderKey;
import com.loopers.core.domain.payment.type.PaymentStatus;
import com.loopers.core.domain.payment.vo.CardNo;
import com.loopers.core.domain.payment.vo.CardType;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.user.vo.UserId;
import org.instancio.Instancio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

@DisplayName("결제")
class PaymentTest {

    @Nested
    @DisplayName("create() 메서드")
    class CreateMethod {

        @Nested
        @DisplayName("유효한 값으로 생성하면")
        class 유효한_값으로_생성 {

            @Test
            @DisplayName("결제 객체를 생성하고 기본값들이 설정된다")
            void 객체_생성_및_기본값_설정() {
                // given
                OrderKey orderKey = Instancio.create(OrderKey.class);
                UserId userId = Instancio.create(UserId.class);
                CardType cardType = new CardType("CREDIT");
                CardNo cardNo = new CardNo("1234567890123456");
                PayAmount amount = new PayAmount(new BigDecimal(10000));

                // when
                Payment payment = Payment.create(orderKey, userId, cardType, cardNo, amount);

                // then
                assertSoftly(softAssertions -> {
                    softAssertions.assertThat(payment.getId()).isNotNull();
                    softAssertions.assertThat(payment.getId().value()).isNull();
                    softAssertions.assertThat(payment.getOrderKey()).isEqualTo(orderKey);
                    softAssertions.assertThat(payment.getUserId()).isEqualTo(userId);
                    softAssertions.assertThat(payment.getCardType()).isEqualTo(cardType);
                    softAssertions.assertThat(payment.getCardNo()).isEqualTo(cardNo);
                    softAssertions.assertThat(payment.getAmount()).isEqualTo(amount);
                    softAssertions.assertThat(payment.getStatus()).isEqualTo(PaymentStatus.PENDING);
                    softAssertions.assertThat(payment.getCreatedAt()).isNotNull();
                    softAssertions.assertThat(payment.getUpdatedAt()).isNotNull();
                    softAssertions.assertThat(payment.getDeletedAt()).isNotNull();
                    softAssertions.assertThat(payment.getDeletedAt().value()).isNull();
                });
            }
        }
    }
}
