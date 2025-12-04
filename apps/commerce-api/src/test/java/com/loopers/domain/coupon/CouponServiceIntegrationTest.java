package com.loopers.domain.coupon;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserFixture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Transactional
@SpringBootTest
class CouponServiceIntegrationTest {
  @Autowired
  private CouponService couponService;

  @Autowired
  private CouponRepository couponRepository;

  @Autowired
  private CouponIssueRepository couponIssueRepository;

  private User user;

  @BeforeEach
  void setUp() {
    user = UserFixture.createUser();
  }

  @Test
  @DisplayName("쿠폰 발급과 사용 - id 기준")
  void assignAndUseCouponById() {
    // given: AMOUNT 쿠폰
    Coupon coupon = Coupon.create(
        CouponPolicyType.AMOUNT,
        new BigDecimal("1000"), // 할인액
        null,
        LocalDateTime.now().minusDays(1),
        LocalDateTime.now().plusDays(1),
        5L
    );
    coupon = couponRepository.save(coupon);

    // when: 쿠폰 발급
    Long issueId = couponService.assignCoupon(coupon.getId(), user.getId());

    // then: 발급 확인
    assertThat(couponIssueRepository.findById(issueId)).isPresent();

    // when: 쿠폰 사용
    BigDecimal price = new BigDecimal("5000");
    BigDecimal discounted = couponService.useCouponById(issueId, user.getId(), price);

    // then: 할인 적용
    assertThat(discounted).isEqualByComparingTo(new BigDecimal("4000"));
    CouponIssue issue = couponIssueRepository.findById(issueId).get();
    assertThat(issue.isUsed()).isTrue();
    assertThat(issue.getCoupon().getUsedCount()).isEqualTo(1L);

    // when: 이미 사용한 쿠폰 재사용 시도
    assertThatThrownBy(() -> couponService.useCouponById(issueId, user.getId(), price))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("사용 불가 쿠폰");
  }

  @Test
  @DisplayName("쿠폰 발급과 사용 - couponCode 기준")
  void assignAndUseCouponByCode() {
    // given: PERCENT 쿠폰
    Coupon coupon = Coupon.create(
        CouponPolicyType.PERCENT,
        null,
        0.1, // 10% 할인
        LocalDateTime.now().minusDays(1),
        LocalDateTime.now().plusDays(1),
        3L
    );
    coupon = couponRepository.save(coupon);

    // when: 쿠폰 발급
    Long issueId = couponService.assignCoupon(coupon.getId(), user.getId());
    CouponIssue issue = couponIssueRepository.findById(issueId).get();
    String code = issue.getCouponCode();

    // then: 발급 확인
    assertThat(code).isNotNull();

    // when: 쿠폰 사용
    BigDecimal price = new BigDecimal("2000");
    BigDecimal discounted = couponService.useCouponByCode(code, user.getId(), price);

    // then: 10% 할인 적용
    assertThat(discounted).isEqualByComparingTo(new BigDecimal("1800"));

    // when: canUseByCode 체크
    boolean canUse = couponService.canUseByCode(code, user.getId());
    assertThat(canUse).isFalse();
  }

  @Test
  @DisplayName("쿠폰 최대 사용 횟수 초과 시 발급 불가")
  void maxUsageExceeded() {
    // given: 사용 횟수 초과 쿠폰
    Coupon coupon = Coupon.create(
        CouponPolicyType.AMOUNT,
        new BigDecimal("500"),
        null,
        LocalDateTime.now().minusDays(1),
        LocalDateTime.now().plusDays(1),
        1L
    );
    coupon.increaseUsed(); // 이미 1회 사용
    Coupon savedCoupon = couponRepository.save(coupon);

    // when, then: 발급 시도
    assertThatThrownBy(() -> couponService.assignCoupon(savedCoupon.getId(), user.getId()))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("발급 불가 쿠폰");
  }

  @Test
  @DisplayName("내 쿠폰 조회")
  void getMyCoupons() {
    // given: 여러 쿠폰 발급
    Coupon c1 = couponRepository.save(Coupon.create(CouponPolicyType.AMOUNT, new BigDecimal("100"), null, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), 5L));
    Coupon c2 = couponRepository.save(Coupon.create(CouponPolicyType.PERCENT, null, 0.2, LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(1), 5L));

    couponService.assignCoupon(c1.getId(), user.getId());
    couponService.assignCoupon(c2.getId(), user.getId());

    // when
    List<CouponIssue> myCoupons = couponService.getMyCoupons(user.getId());

    // then
    assertThat(myCoupons).hasSize(2);
  }
}
