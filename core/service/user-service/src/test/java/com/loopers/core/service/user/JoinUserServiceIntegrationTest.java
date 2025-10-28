package com.loopers.core.service.user;

import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.service.IntegrationTest;
import com.loopers.core.service.user.command.JoinUserCommand;
import org.assertj.core.api.SoftAssertions;
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
    }

}
