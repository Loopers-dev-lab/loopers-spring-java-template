package com.loopers.domain.ranking;

import com.loopers.application.ranking.RankingScheduler;
import com.loopers.domain.ranking.RankingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RankingSchedulerTest {

    @Mock
    private RankingService rankingService;

    @InjectMocks
    private RankingScheduler rankingScheduler;

    @Test
    @DisplayName("prepareNextDayRanking 실행 시 오늘 → 내일로 carryOver가 호출된다")
    void shouldCallCarryOverWithTodayAndTomorrow() {
        // given
        LocalDate fixedDate = LocalDate.of(2025, 1, 15);
        Clock fixedClock = Clock.fixed(
                fixedDate.atTime(23, 50).toInstant(ZoneOffset.of("+09:00")),
                ZoneId.of("Asia/Seoul")
        );

        ReflectionTestUtils.setField(rankingScheduler, "clock", fixedClock);
        ReflectionTestUtils.setField(rankingScheduler, "carryOverWeight", 0.1);

        // when
        rankingScheduler.prepareNextDayRanking();

        // then
        verify(rankingService).carryOverScores(
                LocalDate.of(2025, 1, 15),
                LocalDate.of(2025, 1, 16),
                0.1
        );
    }

    @Test
    @DisplayName("carryOverScores 예외 발생 시에도 정상 종료된다")
    void shouldHandleExceptionGracefully() {
        // given
        LocalDate fixedDate = LocalDate.of(2025, 1, 15);
        Clock fixedClock = Clock.fixed(
                fixedDate.atTime(23, 50).toInstant(ZoneOffset.of("+09:00")),
                ZoneId.of("Asia/Seoul")
        );

        ReflectionTestUtils.setField(rankingScheduler, "clock", fixedClock);
        ReflectionTestUtils.setField(rankingScheduler, "carryOverWeight", 0.1);

        doThrow(new RuntimeException("Redis error"))
                .when(rankingService).carryOverScores(any(), any(), anyDouble());

        // when & then (예외 전파 없이 종료)
        rankingScheduler.prepareNextDayRanking();

        verify(rankingService).carryOverScores(any(), any(), anyDouble());
    }
}
