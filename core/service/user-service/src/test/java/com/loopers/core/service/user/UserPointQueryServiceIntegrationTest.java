package com.loopers.core.service.user;

import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.UserPoint;
import com.loopers.core.domain.user.repository.UserPointRepository;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.user.query.GetUserPointQuery;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserPointQueryServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private UserPointQueryService service;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserPointRepository userPointRepository;

    @Nested
    @DisplayName("getByUserIdentifier()")
    class GetByUserIdentifier {

        @Nested
        @DisplayName("해당 ID의 회원이 존재하지 않을 경우")
        class 해당_ID의_회원이_존재하지_않을_경우 {

            @Test
            @DisplayName("null이 반환된다.")
            void null이_반환된다() {
                UserPoint userPoint = service.getByUserIdentifier(new GetUserPointQuery("non-existent"));

                Assertions.assertThat(userPoint).isNull();
            }
        }

        @Nested
        @DisplayName("해당 ID의 회원이 존재할 경우")
        class 해당_ID의_회원이_존재할_경우 {

            User savedUser;
            UserPoint savedUserPoint;

            @BeforeEach
            void setUp() {
                savedUser = userRepository.save(
                        User.create(
                                UserIdentifier.create("kilian"),
                                UserEmail.create("kilian@gmail.com"),
                                UserBirthDay.create("1997-10-08"),
                                UserGender.create("MALE")
                        )
                );

                savedUserPoint = userPointRepository.save(UserPoint.create(savedUser.getId()));
            }

            @Test
            @DisplayName("보유 포인트가 반환된다.")
            void 보유_포인트가_반환된다() {
                UserPoint userPoint = service.getByUserIdentifier(new GetUserPointQuery("kilian"));

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(userPoint).isNotNull();
                    softly.assertThat(userPoint.getUserId().value()).isEqualTo(savedUser.getId().value());
                    softly.assertThat(userPoint.getBalance().value().intValue()).isEqualTo(0);
                });
            }
        }

    }
}
