package com.loopers.core.service.user;

import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.user.query.GetUserQuery;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserQueryServiceTest extends IntegrationTest {

    @Autowired
    private UserQueryService userQueryService;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("getUserBy()")
    class GetUserBy {

        @Nested
        @DisplayName("ID에 해당하는 사용자가 없는 경우")
        class ID에_해당하는_사용자가_없는_경우 {

            @Test
            @DisplayName("null이 반환된다.")
            void null이_반환된다() {
                User user = userQueryService.getUserBy(new GetUserQuery("identifier"));
                Assertions.assertThat(user).isNull();
            }
        }

        @Nested
        @DisplayName("ID에 해당하는 사용자가 존재할 경우")
        class ID에_해당하는_사용자가_존재할_경우 {

            @BeforeEach
            void setUp() {
                userRepository.save(
                        User.create(
                                UserIdentifier.create("kilian"),
                                UserEmail.create("kilian@gmail.com"),
                                UserBirthDay.create("1997-10-08"),
                                UserGender.create("MALE")
                        )
                );
            }

            @Test
            @DisplayName("회원 정보가 반환된다.")
            void 회원_정보가_반환된다() {
                User user = userQueryService.getUserBy(new GetUserQuery("kilian"));

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(user).isNotNull();
                    softly.assertThat(user.getIdentifier().value()).isEqualTo("kilian");
                    softly.assertThat(user.getEmail().value()).isEqualTo("kilian@gmail.com");
                    softly.assertThat(user.getGender()).isEqualTo(UserGender.MALE);
                });
            }
        }

    }
}
