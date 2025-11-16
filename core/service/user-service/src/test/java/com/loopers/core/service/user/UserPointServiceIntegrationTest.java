package com.loopers.core.service.user;

import com.loopers.core.domain.error.NotFoundException;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.ConcurrencyTestUtil;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.user.command.UserPointChargeCommand;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class UserPointServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private UserPointService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPointRepository userPointRepository;
    @Autowired
    private UserPointService userPointService;

    @Nested
    @DisplayName("포인트 충전")
    class 포인트_충전 {

        @Nested
        @DisplayName("정상적으로 충전할 경우")
        class 정상_충전 {

            User user;

            @BeforeEach
            void setUp() {
                user = userRepository.save(
                        User.create(
                                UserIdentifier.create("kilian"),
                                UserEmail.create("kilian@gmail.com"),
                                UserBirthDay.create("1997-10-08"),
                                UserGender.create("MALE")
                        )
                );
                userPointRepository.save(UserPoint.create(user.getUserId()));
            }

            @Test
            @DisplayName("포인트가 충전된다.")
            void 포인트_충전_성공() {
                // given
                UserPointChargeCommand command = new UserPointChargeCommand("kilian", new BigDecimal(1000));

                // when
                service.charge(command);
                UserPoint chargedPoint = userPointRepository.getByUserId(user.getUserId());

                // then
                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(chargedPoint).isNotNull();
                    softly.assertThat(chargedPoint.getBalance().value().intValue()).isEqualTo(1000);
                });
            }
        }

        @Nested
        @DisplayName("존재하지 않는 유저 ID로 충전을 시도한 경우")
        class 존재하지_않는_유저_ID로_충전 {

            @Test
            @DisplayName("실패한다.")
            void 충전_실패() {
                // given
                UserPointChargeCommand command = new UserPointChargeCommand("nonExist", new BigDecimal(1000));

                // when & then
                assertThatThrownBy(() -> service.charge(command))
                        .isInstanceOf(NotFoundException.class);
            }
        }

        @Nested
        @DisplayName("동시성 테스트")
        class ConcurrencyTest {

            @Nested
            @DisplayName("100명이 동시에 10,000 포인트씩 적립하면")
            class 동시_포인트_적립_정합성 {

                User user;

                @BeforeEach
                void setUp() {
                    user = userRepository.save(
                            User.create(
                                    UserIdentifier.create("kilian"),
                                    UserEmail.create("kilian@gmail.com"),
                                    UserBirthDay.create("1997-10-08"),
                                    UserGender.create("MALE")
                            )
                    );
                    userPointRepository.save(UserPoint.create(user.getUserId()));
                }

                @Test
                @DisplayName("정확히 1,000,000 포인트가 된다")
                void 포인트_충전_결과_확인() throws InterruptedException {
                    // Given
                    int requestCount = 100;
                    BigDecimal chargeAmount = new BigDecimal(10_000);
                    String userIdentifier = "kilian";

                    // When - 100개의 동시 충전 요청 실행
                    List<UserPoint> results = ConcurrencyTestUtil.executeInParallel(
                            requestCount,
                            index -> service.charge(new UserPointChargeCommand(userIdentifier, chargeAmount))
                    );

                    // Then
                    UserPoint userPoint = userPointRepository.getByUserId(user.getUserId());

                    SoftAssertions.assertSoftly(softly -> {
                        softly.assertThat(results).as("동시 요청 결과 수").hasSize(requestCount);
                        softly.assertThat(userPoint.getBalance().value().intValue()).as("최종 포인트 잔액").isEqualTo(1_000_000);
                    });
                }
            }
        }
    }
}
