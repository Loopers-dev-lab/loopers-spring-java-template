package com.loopers.interfaces.api;

import com.loopers.application.point.PointFacade;
import com.loopers.application.point.PointInfo;
import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.point.PointV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PointV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final PointRepository pointRepository;
    private final DatabaseCleanUp databaseCleanUp;
    private final UserRepository userRepository;
    private final PointFacade pointFacade;

    @Autowired
    public PointV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            PointRepository pointRepository,
            DatabaseCleanUp databaseCleanUp,
            UserRepository userRepository,
            PointFacade pointFacade
    ) {
        this.testRestTemplate = testRestTemplate;
        this.pointRepository = pointRepository;
        this.databaseCleanUp = databaseCleanUp;
        this.userRepository = userRepository;
        this.pointFacade = pointFacade;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
    @Test
    void returnsChargedPointBalance_whenChargePointSuccess() {
        // arrange
        UserEntity existedUser = userRepository.save(
                new UserEntity(
                        "happy97",
                        "happygimy97@naver.com",
                        Gender.MALE,
                        "1997-09-23",
                        "test1234!"
                )
        );

        // 초기 포인트 500L 세팅
        Point existedPoint = pointRepository.save(new Point(existedUser.getId(), 500L));

        PointV1Dto.PointChargeRequest request = new PointV1Dto.PointChargeRequest(
                existedUser.getId(),
                1000L
        );

        // act
        PointInfo pointInfo = pointFacade.chargePoint(request);

        // assert
        assertAll(
                () -> assertThat(pointInfo).isNotNull(),
                () -> assertThat(pointInfo.userId()).isEqualTo(existedUser.getId()),
                () -> assertThat(pointInfo.balance()).isEqualTo(1500L)  // 기존 500 + 1000 충전
        );

        // 실제 DB 확인 (신뢰성 double-check)
        Point found = pointRepository.findByUserId(existedUser.getId())
                .orElseThrow(() -> new AssertionError("DB에 포인트가 존재하지 않습니다."));
        assertThat(found.getBalance()).isEqualTo(1500L);
    }

}
