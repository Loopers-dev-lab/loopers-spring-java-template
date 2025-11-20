package com.loopers.core.service.order.component;

import com.loopers.core.domain.order.AmountDiscountCoupon;
import com.loopers.core.domain.order.repository.CouponRepository;
import com.loopers.core.domain.order.type.CouponStatus;
import com.loopers.core.domain.order.vo.AmountDiscountCouponId;
import com.loopers.core.domain.order.vo.CouponDiscountAmount;
import com.loopers.core.domain.order.vo.CouponId;
import com.loopers.core.domain.payment.vo.PayAmount;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.service.ConcurrencyTestUtil;
import com.loopers.core.service.IntegrationTest;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.instancio.Select.field;

@DisplayName("쿠폰 할인전략")
class CouponDiscountStrategyTest extends IntegrationTest {

    @Autowired
    private CouponRepository couponRepository;

    @Autowired
    private CouponDiscountStrategy discountStrategy;

    @Nested
    @DisplayName("discount()")
    class DiscountMethod {

        @Nested
        @DisplayName("쿠폰이 존재하면")
        class couponExist {

            CouponId couponId;

            @BeforeEach
            void setUp() {
                AmountDiscountCoupon coupon = (AmountDiscountCoupon) couponRepository.save(
                        Instancio.of(AmountDiscountCoupon.class)
                                .set(field(AmountDiscountCoupon::getCouponId), CouponId.empty())
                                .set(field(AmountDiscountCoupon::getId), AmountDiscountCouponId.empty())
                                .set(field(AmountDiscountCoupon::getUserId), new UserId("1"))
                                .set(field(AmountDiscountCoupon::getAmount), new CouponDiscountAmount(new BigDecimal(1000)))
                                .set(field(AmountDiscountCoupon::getStatus), CouponStatus.AVAILABLE)
                                .create()
                );
                couponId = coupon.getCouponId();
            }

            @Test
            @DisplayName("쿠폰을 사용하고 할인된다.")
            void usedCouponAndDiscount() {
                PayAmount payAmount = new PayAmount(new BigDecimal(10000));
                PayAmount discounted = discountStrategy.discount(payAmount, couponId);

                AmountDiscountCoupon usedCoupon = (AmountDiscountCoupon) couponRepository.getById(couponId);

                assertSoftly(softly -> {
                    softly.assertThat(discounted.value()).isEqualByComparingTo(new BigDecimal(9000));
                    softly.assertThat(usedCoupon.getStatus()).isEqualTo(CouponStatus.USED);
                });
            }
        }

        @Nested
        @DisplayName("동시에 하나의 쿠폰을 여러번 사용하려고 하면")
        class concurrentlyUseCoupon {

            CouponId couponId;

            @BeforeEach
            void setUp() {
                AmountDiscountCoupon coupon = (AmountDiscountCoupon) couponRepository.save(
                        Instancio.of(AmountDiscountCoupon.class)
                                .set(field(AmountDiscountCoupon::getCouponId), CouponId.empty())
                                .set(field(AmountDiscountCoupon::getId), AmountDiscountCouponId.empty())
                                .set(field(AmountDiscountCoupon::getUserId), new UserId("1"))
                                .set(field(AmountDiscountCoupon::getAmount), new CouponDiscountAmount(new BigDecimal(1000)))
                                .set(field(AmountDiscountCoupon::getStatus), CouponStatus.AVAILABLE)
                                .create()
                );
                couponId = coupon.getCouponId();
            }

            @Test
            @DisplayName("한번만 사용된다.")
            void usedOnce() throws InterruptedException {
                int requestCount = 100;
                List<PayAmount> payAmounts = ConcurrencyTestUtil.executeInParallel(
                        requestCount,
                        index -> {
                            PayAmount payAmount = new PayAmount(new BigDecimal(10000));
                            return discountStrategy.discount(payAmount, couponId);
                        }
                );

                List<PayAmount> discountedPayAmounts = payAmounts.stream()
                        .filter(payAmount -> payAmount.value().compareTo(new BigDecimal(9000)) == 0)
                        .toList();
                assertThat(discountedPayAmounts).hasSize(1);
            }
        }
    }

}
