package com.loopers.batch.job.ranking.writer;

import com.loopers.batch.job.ranking.dto.RankingAggregation;
import com.loopers.domain.ranking.WeeklyRankEntity;
import com.loopers.domain.ranking.WeeklyRankRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyRankWriter 단위 테스트")
class WeeklyRankWriterUnitTest {

    @Mock
    private WeeklyRankRepository weeklyRankRepository;

    @InjectMocks
    private WeeklyRankWriter weeklyRankWriter;

    private static final String TEST_YEAR_WEEK = "2024-W52";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(weeklyRankWriter, "yearWeek", TEST_YEAR_WEEK);
    }

    @Nested
    @DisplayName("주간 랭킹 저장")
    class 주간_랭킹_저장 {

        @Test
        @DisplayName("랭킹 데이터를 정상적으로 저장한다")
        void should_save_weekly_ranking_successfully() throws Exception {
            // given
            List<RankingAggregation> aggregations = List.of(
                    new RankingAggregation(1L, 100L, 50L, 10L, 5L, 310L, 1),
                    new RankingAggregation(2L, 80L, 40L, 8L, 4L, 248L, 2)
            );
            Chunk<RankingAggregation> chunk = new Chunk<>(aggregations);

            when(weeklyRankRepository.deleteByYearWeek(TEST_YEAR_WEEK)).thenReturn(2L);
            when(weeklyRankRepository.saveAll(anyList())).thenReturn(List.of());

            // when
            weeklyRankWriter.write(chunk);

            // then
            verify(weeklyRankRepository).deleteByYearWeek(TEST_YEAR_WEEK);
            verify(weeklyRankRepository).saveAll(any());
        }

        @Test
        @DisplayName("빈 청크인 경우 저장하지 않는다")
        void should_not_save_when_chunk_is_empty() throws Exception {
            // given
            Chunk<RankingAggregation> emptyChunk = new Chunk<>();

            // when
            weeklyRankWriter.write(emptyChunk);

            // then
            verify(weeklyRankRepository).deleteByYearWeek(TEST_YEAR_WEEK);
            verify(weeklyRankRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("저장 중 예외 발생 시 RuntimeException을 던진다")
        void should_throw_runtime_exception_when_save_fails() {
            // given
            List<RankingAggregation> aggregations = List.of(
                    new RankingAggregation(1L, 100L, 50L, 10L, 5L, 310L, 1)
            );
            Chunk<RankingAggregation> chunk = new Chunk<>(aggregations);

            when(weeklyRankRepository.deleteByYearWeek(TEST_YEAR_WEEK)).thenReturn(1L);
            when(weeklyRankRepository.saveAll(anyList())).thenThrow(new RuntimeException("DB 저장 실패"));

            // when & then
            assertThatThrownBy(() -> weeklyRankWriter.write(chunk))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("주간 랭킹 저장 실패");
        }
    }

    @Nested
    @DisplayName("Entity 변환")
    class Entity_변환 {

        @Test
        @DisplayName("RankingAggregation을 WeeklyRankEntity로 올바르게 변환한다")
        void should_convert_aggregation_to_entity_correctly() throws Exception {
            // given
            RankingAggregation aggregation = new RankingAggregation(1L, 100L, 50L, 10L, 5L, 310L, 1);
            List<WeeklyRankEntity> expectedEntities = List.of(
                    WeeklyRankEntity.create(1L, TEST_YEAR_WEEK, 100L, 50L, 10L, 5L, 310L, 1)
            );

            when(weeklyRankRepository.deleteByYearWeek(TEST_YEAR_WEEK)).thenReturn(0L);
            when(weeklyRankRepository.saveAll(anyList())).thenReturn(expectedEntities);

            // when
            weeklyRankWriter.write(new Chunk<>(List.of(aggregation)));

            // then
            verify(weeklyRankRepository).saveAll(expectedEntities);
        }
    }
}