package com.loopers.batch.job.ranking.writer;

import com.loopers.batch.job.ranking.dto.RankingAggregation;
import com.loopers.domain.ranking.MonthlyRankEntity;
import com.loopers.domain.ranking.MonthlyRankRepository;
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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("MonthlyRankWriter 단위 테스트")
class MonthlyRankWriterUnitTest {

    @Mock
    private MonthlyRankRepository monthlyRankRepository;

    @InjectMocks
    private MonthlyRankWriter monthlyRankWriter;

    private static final String TEST_YEAR_MONTH = "2024-12";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(monthlyRankWriter, "yearMonth", TEST_YEAR_MONTH);
    }

    @Nested
    @DisplayName("월간 랭킹 저장")
    class 월간_랭킹_저장 {

        @Test
        @DisplayName("랭킹 데이터를 정상적으로 저장한다")
        void should_save_monthly_ranking_successfully() throws Exception {
            // given
            List<RankingAggregation> aggregations = List.of(
                    new RankingAggregation(1L, 1000L, 500L, 100L, 50L, 3100L, 1),
                    new RankingAggregation(2L, 800L, 400L, 80L, 40L, 2480L, 2)
            );
            Chunk<RankingAggregation> chunk = new Chunk<>(aggregations);

            when(monthlyRankRepository.deleteByYearMonth(TEST_YEAR_MONTH)).thenReturn(2L);
            when(monthlyRankRepository.saveAll(anyList())).thenReturn(List.of());

            // when
            monthlyRankWriter.write(chunk);

            // then
            verify(monthlyRankRepository).deleteByYearMonth(TEST_YEAR_MONTH);
            verify(monthlyRankRepository).saveAll(any());
        }

        @Test
        @DisplayName("빈 청크인 경우 저장하지 않는다")
        void should_not_save_when_chunk_is_empty() throws Exception {
            // given
            Chunk<RankingAggregation> emptyChunk = new Chunk<>();

            // when
            monthlyRankWriter.write(emptyChunk);

            // then
            verify(monthlyRankRepository).deleteByYearMonth(TEST_YEAR_MONTH);
            verify(monthlyRankRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("저장 중 예외 발생 시 RuntimeException을 던진다")
        void should_throw_runtime_exception_when_save_fails() {
            // given
            List<RankingAggregation> aggregations = List.of(
                    new RankingAggregation(1L, 1000L, 500L, 100L, 50L, 3100L, 1)
            );
            Chunk<RankingAggregation> chunk = new Chunk<>(aggregations);

            when(monthlyRankRepository.deleteByYearMonth(TEST_YEAR_MONTH)).thenReturn(1L);
            when(monthlyRankRepository.saveAll(anyList())).thenThrow(new RuntimeException("DB 저장 실패"));

            // when & then
            assertThatThrownBy(() -> monthlyRankWriter.write(chunk))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("월간 랭킹 저장 실패");
        }
    }

    @Nested
    @DisplayName("Entity 변환")
    class Entity_변환 {

        @Test
        @DisplayName("RankingAggregation을 MonthlyRankEntity로 올바르게 변환한다")
        void should_convert_aggregation_to_entity_correctly() throws Exception {
            // given
            RankingAggregation aggregation = new RankingAggregation(1L, 1000L, 500L, 100L, 50L, 3100L, 1);
            List<MonthlyRankEntity> expectedEntities = List.of(
                    MonthlyRankEntity.create(1L, TEST_YEAR_MONTH, 1000L, 500L, 100L, 50L, 3100L, 1)
            );

            when(monthlyRankRepository.deleteByYearMonth(TEST_YEAR_MONTH)).thenReturn(0L);
            when(monthlyRankRepository.saveAll(anyList())).thenReturn(expectedEntities);

            // when
            monthlyRankWriter.write(new Chunk<>(List.of(aggregation)));

            // then
            verify(monthlyRankRepository).saveAll(expectedEntities);
        }
    }
}