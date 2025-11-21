package com.loopers.domain.coupon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import com.loopers.domain.user.User;
import com.loopers.domain.user.User.Gender;
import com.loopers.domain.user.UserRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CouponServiceIntegrationTest {

  @Autowired
  CouponService couponService;

  @MockitoSpyBean
  CouponRepository couponRepository;

  @MockitoSpyBean
  UserRepository userRepository;

  @Autowired
  private DatabaseCleanUp databaseCleanUp;

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("쿠폰 조회 시,")
  @Nested
  class GetCoupon {

    @DisplayName("존재하는 쿠폰이면 조회된다.")
    @Test
    void getCoupon_success_whenExists() {
      // arrange
      User user = userRepository.save(new User("user1", "a@email.com", "2025-11-11", Gender.FEMALE));
      Coupon coupon = couponRepository.save(
          new Coupon(user.getId(), CouponType.FIXED_AMOUNT, 10)
      );

      // act
      Coupon found = couponService.getCoupon(coupon.getId());

      // assert
      assertThat(found).isNotNull();
      assertThat(found.getId()).isEqualTo(coupon.getId());
    }

    @DisplayName("존재하지 않는 쿠폰이면 예외가 발생한다.")
    @Test
    void getCoupon_fail_whenNotFound() {
      assertThrows(RuntimeException.class, () -> couponService.getCoupon(999L));
    }
  }
}
