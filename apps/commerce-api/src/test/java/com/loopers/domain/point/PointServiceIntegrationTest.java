package com.loopers.domain.point;

import com.loopers.application.point.PointInfo;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.point.PointAccountJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@SpringBootTest
class PointServiceIntegrationTest {

    private static final String USER_ID = "abc123";
    private static final String EMAIL = "abc@sample.com";
    private static final String BIRTH_DATE = "2000-01-01";
    private final Gender GENDER = Gender.FEMALE;

    @Autowired
    private PointService pointService;

    @MockitoSpyBean
    private UserJpaRepository userJpaRepository;

    @MockitoSpyBean
    private PointAccountJpaRepository pointAccountJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("포인트 조회")
    @Nested
    class GetBalance {

        @Test
        @DisplayName("해당 ID 의 회원이 존재할 경우, 보유 포인트가 반환된다")
        void pointTest1() {
            // arrange
            UserModel user = userJpaRepository.save(UserModel.create(USER_ID, EMAIL, BIRTH_DATE, GENDER));

            // act
            Point balance = pointService.getBalance(user.getUserId());

            // assert
            assertAll(
                    () -> assertThat(balance).isNotNull(),
                    () -> assertThat(balance.amount()).isEqualTo(0L)
            );
        }

    }
}
