package com.loopers.application.point;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
import com.loopers.utils.DatabaseCleanUp;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PointFacadeIntegrationTest {
    @Autowired
    private PointFacade pointFacade;
    @Autowired
    private UserJpaRepository userJpaRepository;
    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("포인트를 조회할 때, ")
    @Nested
    class Get {

        @DisplayName("존재하는 user ID를 주면, 해당 포인트 정보를 반환한다.")
        @Test
        void returnsPointInfo_whenValidUserModelIdIsProvided() {
            // arrange
            String userId = "test";
            String email = "test@test.com";
            LocalDate birthday = LocalDate.of(2020, 1, 1);
            User saveUser = User.create(userId, email, birthday, Gender.MALE);
            User user = userJpaRepository.save(saveUser);

            // act
            PointInfo result = pointFacade.getOrCreatePointByUserModelId(user.getId());

            // assert
            assertAll(
                    () -> assertThat(result).isNotNull(),
                    () -> assertThat(result.id()).isNotNull(),
                    () -> assertThat(result.userModelId()).isEqualTo(user.getId()),
                    () -> assertThat(result.point()).isEqualTo(0L)
            );
        }
    }

    @DisplayName("포인트를 충전할 때, ")
    @Nested
    class Charge {
        @DisplayName("존재하지 않는 유저 PK ID 로 충전을 시도한 경우, CoreException 예외가 발생한다.")
        @Test
        void throwsCoreException_whenNonExistingUserIdIsProvided() {
            // arrange
            Long userModelId = -1L;
            PointInfo pointInfo = new PointInfo(null, userModelId, 100L);

            // act & assert
            assertThatThrownBy(() -> pointFacade.charge(pointInfo))
                    .isInstanceOf(CoreException.class);
        }
    }
}
