package com.loopers.domain.points;

import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class PointServiceIntegrationTest {
    @Autowired
    private PointService pointService;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("포인트 조회 시,")
    @Nested
    class GetMemberPoints {
        
        @DisplayName("해당 ID의 회원이 존재할 경우, 보유 포인트가 반환된다")
        @Test
        void shouldReturnPoints_whenMemberExists() {
            String memberId = "test123";
            pointService.initializeMemberPoints(memberId);

            BigDecimal result = pointService.getMemberPoints(memberId);

            assertThat(result).isEqualTo(BigDecimal.ZERO);
        }

        @DisplayName("해당 ID의 회원이 존재하지 않을 경우, null이 반환된다")
        @Test
        void shouldReturnNull_whenMemberDoesNotExist() {
            BigDecimal result = pointService.getMemberPoints("nonexistent");

            assertThat(result).isNull();
        }
    }
}