package com.loopers.domain.point;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.point.PointAccountJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.support.error.CoreException;
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

@SpringBootTest
class PointServiceIntegrationTest {

    private static final String USER_ID = "abc123";
    private static final String EMAIL = "abc@sample.com";
    private static final String BIRTH_DATE = "2000-01-01";
    private final Gender GENDER = Gender.FEMALE;
    private static final long CHARGE_AMOUNT = 1_000L;


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
            User user = userJpaRepository.save(User.create(USER_ID, EMAIL, BIRTH_DATE, GENDER));

            // act
            Point balance = pointService.getBalance(user.getUserId());

            // assert
            assertAll(() -> assertThat(balance).isNotNull(), () -> assertThat(balance.amount()).isEqualTo(0L));
        }

        @Test
        @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
        void pointTest2() {

            // act
            Point balance = pointService.getBalance(USER_ID);

            // assert
            assertAll(() -> assertThat(balance).isNull());
        }
    }

    @DisplayName("포인트 충전")
    @Nested
    class Charge {

        @DisplayName("존재하지 않는 유저 ID 로 충전을 시도한 경우, 실패한다.")
        @Test
        void pointTest3() {

            assertThatThrownBy(() -> pointService.charge(USER_ID, CHARGE_AMOUNT))
                    .isInstanceOf(CoreException.class)
                    .hasMessageContaining("존재하지 않는 유저 입니다.");
        }
    }


}
