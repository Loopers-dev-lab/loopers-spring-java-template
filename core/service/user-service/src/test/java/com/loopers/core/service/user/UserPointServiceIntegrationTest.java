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
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.user.command.UserPointChargeCommand;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserPointServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private UserPointService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPointRepository userPointRepository;

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
                UserPointChargeCommand command = new UserPointChargeCommand("kilian", 1000);

                // when
                service.charge(command);
                UserPoint chargedPoint = userPointRepository.getByUserId(user.getUserId());

                // then
                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(chargedPoint).isNotNull();
                    softly.assertThat(chargedPoint.getBalance().value()).isEqualTo(1000);
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
                UserPointChargeCommand command = new UserPointChargeCommand("nonExist", 1000);

                // when & then
                Assertions.assertThatThrownBy(() -> service.charge(command))
                        .isInstanceOf(NotFoundException.class);
            }
        }
    }

}
