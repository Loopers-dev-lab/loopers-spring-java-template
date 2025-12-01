package com.loopers.domain.point;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class PointServiceIntegrationTest {

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PointService pointService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("포인트 조회 통합 테스트")
    @Nested
    class PointUser {

        @DisplayName("해당 ID의 회원이 존재할 경우, 보유 포인트가 반환된다.")
        @Test
        void returnPointInfo_whenValidIdIsProvided() {
            //given
            String id = "yh45g";
            String email = "yh45g@loopers.com";
            String birth = "1994-12-05";
            String gender = "MALE";

            userRepository.save(new User(id, email, birth, gender));
            pointRepository.save(Point.create(id, 0L));

            //when
            Point result = pointService.findPointByUserId(id);

            //then
            assertThat(result.getUserId()).isEqualTo(id);
            assertThat(result.getBalance()).isEqualTo(0L);
        }

        @DisplayName("회원이 존재 하지 않을 경우, null 이 반환된다.")
        @Test
        void returnNull_whenInvalidUserIdIsProvided() {
            //given
            String id = "yh45g";

            //when
            Point point = pointService.findPointByUserId(id);

            //then
            assertThat(point).isNull();
        }
    }

    @DisplayName("포인트 충전 통합 테스트")
    @Nested
    class Charge {

        @DisplayName("존재하지 않는 유저 ID 로 충전을 시도한 경우, 실패한다.")
        @Test
        void throwsChargeAmountFailException_whenUserIDIsNotProvided() {
            //given
            String id = "yh45g";

            //when
            CoreException exception = assertThrows(CoreException.class, () -> pointService.chargePoint(id, 1000L));

            //then
            assertThat(exception.getErrorType()).isEqualTo(ErrorType.NOT_FOUND);
        }

        @Test
        @DisplayName("회원이 존재하면 포인트 충전 성공")
        void chargeSuccess() {
            // given
            String userId = "user2";
            userRepository.save(new User(userId, "yh45g@loopers.com", "1994-12-05", "MALE"));
            pointRepository.save(Point.create(userId, 1000L));

            // when
            Point updated = pointService.chargePoint(userId, 500L);

            // then
            assertThat(updated.getBalance()).isEqualTo(1500L);
        }
    }
}
