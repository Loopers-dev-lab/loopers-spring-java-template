package com.loopers.domain.coupon;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringBootTest
class CouponServiceIntegrationTest {

    @Autowired
    private CouponService couponService;

    @MockitoSpyBean
    private CouponRepository couponRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        user1 = userService.signUp("ser01", "coupon1@mail.com", "1990-01-01", Gender.MALE);
        user2 = userService.signUp("user02", "coupon2@mail.com", "1990-01-02", Gender.FEMALE);
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("쿠폰 저장")
    @Nested
    class SaveCoupon {

        @DisplayName("쿠폰을 저장할 수 있다.")
        @Test
        void shouldSaveCouponSuccessfully() {
            // arrange
            Coupon coupon = Coupon.create(user1, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L);

            // act
            Coupon savedCoupon = couponService.save(coupon);

            // assert
            verify(couponRepository, times(1)).save(any(Coupon.class));
            assertAll(
                    () -> assertThat(savedCoupon).isNotNull(),
                    () -> assertThat(savedCoupon.getId()).isNotNull(),
                    () -> assertThat(savedCoupon.getUser()).isEqualTo(user1),
                    () -> assertThat(savedCoupon.getName()).isEqualTo("5000원 할인"),
                    () -> assertThat(savedCoupon.getIsUsed()).isFalse()
            );
        }
    }

    @DisplayName("쿠폰 조회 (비관적 락)")
    @Nested
    class GetCouponWithPessimisticLock {

        @DisplayName("비관적 락으로 쿠폰을 조회할 수 있다.")
        @Test
        void shouldRetrieveCouponWithPessimisticLock_successfully() {
            // arrange
            Coupon coupon = Coupon.create(user1, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L);
            Coupon savedCoupon = couponService.save(coupon);

            // act
            Coupon foundCoupon = couponService.getCouponWithPessimisticLock(savedCoupon.getId());

            // assert
            verify(couponRepository, times(1)).findByIdWithPessimisticLock(savedCoupon.getId());
            assertAll(
                    () -> assertThat(foundCoupon).isNotNull(),
                    () -> assertThat(foundCoupon.getId()).isEqualTo(savedCoupon.getId()),
                    () -> assertThat(foundCoupon.getName()).isEqualTo("5000원 할인")
            );
        }

        @DisplayName("존재하지 않는 쿠폰 ID로 조회 시 NOT_FOUND 예외가 발생한다.")
        @Test
        void throwsNotFoundException_whenCouponNotExists() {
            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                couponService.getCouponWithPessimisticLock(-99L);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }
    }

    @DisplayName("쿠폰 조회 (낙관적 락)")
    @Nested
    class GetCouponWithOptimisticLock {

        @DisplayName("낙관적 락으로 쿠폰을 조회할 수 있다.")
        @Test
        void getCouponWithOptimisticLock() {
            // arrange
            Coupon coupon = Coupon.create(user1, "20% 할인", DiscountType.PERCENTAGE, 20L);
            Coupon savedCoupon = couponService.save(coupon);

            // act
            Coupon foundCoupon = couponService.getCouponWithOptimisticLock(savedCoupon.getId());

            // assert
            verify(couponRepository, times(1)).findByIdWithOptimisticLock(savedCoupon.getId());
            assertAll(
                    () -> assertThat(foundCoupon).isNotNull(),
                    () -> assertThat(foundCoupon.getId()).isEqualTo(savedCoupon.getId()),
                    () -> assertThat(foundCoupon.getName()).isEqualTo("20% 할인")
            );
        }
    }

    @DisplayName("쿠폰 사용 가능 여부 검증 (validateCouponUsable)")
    @Nested
    class ValidateCouponUsable {

        @DisplayName("본인 소유의 미사용 쿠폰은 검증을 통과한다.")
        @Test
        void validateCouponUsable_success() {
            // arrange
            Coupon coupon = Coupon.create(user1, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L);
            Coupon savedCoupon = couponService.save(coupon);

            // act & assert - 예외 발생하지 않음
            couponService.validateCouponUsable(savedCoupon, user1);
        }

        @DisplayName("타인 소유의 쿠폰은 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenNotOwner() {
            // arrange
            Coupon coupon = Coupon.create(user1, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L);
            Coupon savedCoupon = couponService.save(coupon);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                couponService.validateCouponUsable(savedCoupon, user2);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }

        @DisplayName("이미 사용된 쿠폰은 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsBadRequestException_whenAlreadyUsed() {
            // arrange
            Coupon coupon = Coupon.create(user1, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L);
            coupon.use(); // 쿠폰 사용 처리
            Coupon savedCoupon = couponService.save(coupon);

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                couponService.validateCouponUsable(savedCoupon, user1);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
        }
    }

    @DisplayName("쿠폰 사용 흐름")
    @Nested
    class CouponUsageFlow {

        @DisplayName("쿠폰을 사용하고 저장하면 데이터베이스에 반영된다.")
        @Test
        void useCoupon_andSave() {
            // arrange
            Coupon coupon = Coupon.create(user1, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L);
            Coupon savedCoupon = couponService.save(coupon);

            // act
            savedCoupon.use();
            couponService.save(savedCoupon);

            // assert
            Coupon foundCoupon = couponRepository.findById(savedCoupon.getId()).orElseThrow();
            assertAll(
                    () -> assertThat(foundCoupon.getIsUsed()).isTrue(),
                    () -> assertThat(foundCoupon.canUse()).isFalse()
            );
        }

        @DisplayName("쿠폰 사용 후 재사용 시도 시 BAD_REQUEST 예외가 발생한다.")
        @Test
        void throwsException_whenReuseUsedCoupon() {
            // arrange
            Coupon coupon = Coupon.create(user1, "5000원 할인", DiscountType.FIXED_AMOUNT, 5000L);
            Coupon savedCoupon = couponService.save(coupon);
            savedCoupon.use();
            couponService.save(savedCoupon);

            // act & assert
            Coupon foundCoupon = couponRepository.findById(savedCoupon.getId()).orElseThrow();
            assertThrows(CoreException.class, foundCoupon::use);
        }
    }
}
