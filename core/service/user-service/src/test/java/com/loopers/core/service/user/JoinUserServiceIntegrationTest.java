package com.loopers.core.service.user;

import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.user.command.JoinUserCommand;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Optional;

class JoinUserServiceIntegrationTest extends IntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JoinUserService joinUserService;

    @Nested
    @DisplayName("회원가입 시")
    class 회원_가입 {

        @Nested
        @DisplayName("유효한 경우")
        class 유효한_경우 {

            @Test
            @DisplayName("회원이 저장된다.")
            void 회원_저장() {
                JoinUserCommand joinUserCommand = new JoinUserCommand(
                        "user123",
                        "kilian@gmail.com",
                        "2000-01-15",
                        "MALE"
                );

                User user = joinUserService.joinUser(joinUserCommand);
                Optional<User> findUser = userRepository.findById(user.getUserId());

                SoftAssertions.assertSoftly(softly -> {
                    softly.assertThat(findUser.isPresent()).isTrue();
                    softly.assertThat(findUser.get().getIdentifier().value()).isEqualTo(joinUserCommand.getUserIdentifier());
                });
            }
        }

        @Nested
        @DisplayName("이미 존재하는 회원의 ID인 경우")
        class 이미_존재하는_회원의_ID인_경우 {

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
            @DisplayName("IllegalArgumentException이 발생한다.")
            void 예외_발생() {
                JoinUserCommand command = new JoinUserCommand("kilian", "ddd@gmail.com", "2000-01-15", "FEMALE");
                Assertions.assertThatThrownBy(() -> joinUserService.joinUser(command))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage("이미 존재하는 사용자 ID입니다.");
            }
        }
    }
}
