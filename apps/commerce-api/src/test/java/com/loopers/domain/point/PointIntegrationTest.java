package com.loopers.domain.point;

import com.loopers.infrastructure.point.PointJpaRepository;
import static org.assertj.core.api.Assertions.assertThat;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

@SpringBootTest
@AutoConfigureMockMvc
public class PointIntegrationTest {

    @Autowired
    private PointService pointService;

    @MockitoSpyBean
    private PointJpaRepository pointJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Test
    @DisplayName("해당 ID 의 회원이 존재할 경우, 보유 포인트가 반환된다.")
    void return_point_when_user_exist() {
        //given
        String userId = "sangdon";
        Long expectedPointAmount = 1000L;
        
        Point point = Point.builder()
                .id(userId)
                .pointAmount(expectedPointAmount)
                .build();
        
        pointJpaRepository.save(point);

        //when
        Point result = pointService.getPoints(userId);

        //then
        assertThat(result).isNotNull();
        assertThat(result.getPointAmount()).isEqualTo(expectedPointAmount);
        assertThat(result.getId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("해당 ID 의 회원이 존재하지 않을 경우, null 이 반환된다.")
    void return_null_when_user_not_exist() {
        //given
        String nonExistentUserId = "nonexistent";

        //when
        Point result = pointService.getPoints(nonExistentUserId);

        //then
        assertThat(result).isNull();
    }
}
