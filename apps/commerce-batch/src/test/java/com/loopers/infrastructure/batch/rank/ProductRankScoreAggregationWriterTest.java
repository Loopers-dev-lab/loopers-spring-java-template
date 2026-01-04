package com.loopers.infrastructure.batch.rank;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.rank.ProductRankScore;
import com.loopers.domain.rank.ProductRankScoreRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

/**
 * ProductRankScoreAggregationWriter 테스트.
 */
@ExtendWith(MockitoExtension.class)
class ProductRankScoreAggregationWriterTest {

    @Mock
    private ProductRankScoreRepository productRankScoreRepository;

    @InjectMocks
    private ProductRankScoreAggregationWriter writer;

    @DisplayName("Chunk 내에서 같은 product_id를 가진 메트릭을 집계한다")
    @Test
    void aggregatesMetricsByProductId() throws Exception {
        // arrange
        List<ProductMetrics> items = new ArrayList<>();
        
        // 같은 product_id를 가진 메트릭 2개
        ProductMetrics metrics1 = new ProductMetrics(1L);
        metrics1.incrementLikeCount();
        metrics1.incrementSalesCount(10);
        metrics1.incrementViewCount();
        items.add(metrics1);
        
        ProductMetrics metrics2 = new ProductMetrics(1L);
        metrics2.incrementLikeCount();
        metrics2.incrementSalesCount(20);
        metrics2.incrementViewCount();
        items.add(metrics2);
        
        // 다른 product_id
        ProductMetrics metrics3 = new ProductMetrics(2L);
        metrics3.incrementLikeCount();
        items.add(metrics3);
        
        Chunk<ProductMetrics> chunk = new Chunk<>(items);
        
        when(productRankScoreRepository.findAllByProductIdIn(anySet())).thenReturn(List.of());
        doNothing().when(productRankScoreRepository).saveAll(anyList());

        // act
        writer.write(chunk);

        // assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductRankScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRankScoreRepository, times(1)).saveAll(captor.capture());
        
        List<ProductRankScore> savedScores = captor.getValue();
        assertThat(savedScores).hasSize(2);
        
        // product_id=1: 좋아요 2, 판매량 30, 조회수 2
        ProductRankScore score1 = savedScores.stream()
            .filter(s -> s.getProductId().equals(1L))
            .findFirst()
            .orElseThrow();
        assertThat(score1.getLikeCount()).isEqualTo(2L);
        assertThat(score1.getSalesCount()).isEqualTo(30L);
        assertThat(score1.getViewCount()).isEqualTo(2L);
        
        // product_id=2: 좋아요 1, 판매량 0, 조회수 0
        ProductRankScore score2 = savedScores.stream()
            .filter(s -> s.getProductId().equals(2L))
            .findFirst()
            .orElseThrow();
        assertThat(score2.getLikeCount()).isEqualTo(1L);
        assertThat(score2.getSalesCount()).isEqualTo(0L);
        assertThat(score2.getViewCount()).isEqualTo(0L);
    }

    @DisplayName("점수를 올바른 가중치로 계산한다")
    @Test
    void calculatesScoreWithCorrectWeights() throws Exception {
        // arrange
        List<ProductMetrics> items = new ArrayList<>();
        
        ProductMetrics metrics = new ProductMetrics(1L);
        metrics.incrementLikeCount();  // 1
        metrics.incrementSalesCount(10); // 10
        metrics.incrementViewCount();   // 1
        items.add(metrics);
        
        Chunk<ProductMetrics> chunk = new Chunk<>(items);
        
        when(productRankScoreRepository.findAllByProductIdIn(anySet())).thenReturn(List.of());
        doNothing().when(productRankScoreRepository).saveAll(anyList());

        // act
        writer.write(chunk);

        // assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductRankScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRankScoreRepository, times(1)).saveAll(captor.capture());
        
        ProductRankScore savedScore = captor.getValue().get(0);
        // 점수 = 1 * 0.3 + 10 * 0.5 + 1 * 0.2 = 0.3 + 5.0 + 0.2 = 5.5
        assertThat(savedScore.getScore()).isEqualTo(5.5);
    }

    @DisplayName("기존 데이터가 있으면 누적하여 저장한다")
    @Test
    void accumulatesWithExistingData() throws Exception {
        // arrange
        List<ProductMetrics> items = new ArrayList<>();
        
        ProductMetrics metrics = new ProductMetrics(1L);
        metrics.incrementLikeCount();
        metrics.incrementSalesCount(10);
        metrics.incrementViewCount();
        items.add(metrics);
        
        Chunk<ProductMetrics> chunk = new Chunk<>(items);
        
        // 기존 데이터: 좋아요 5, 판매량 20, 조회수 3
        ProductRankScore existingScore = new ProductRankScore(1L, 5L, 20L, 3L, 12.1);
        when(productRankScoreRepository.findAllByProductIdIn(anySet())).thenReturn(List.of(existingScore));
        doNothing().when(productRankScoreRepository).saveAll(anyList());

        // act
        writer.write(chunk);

        // assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductRankScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRankScoreRepository, times(1)).saveAll(captor.capture());
        
        ProductRankScore savedScore = captor.getValue().get(0);
        // 누적: 좋아요 5+1=6, 판매량 20+10=30, 조회수 3+1=4
        assertThat(savedScore.getLikeCount()).isEqualTo(6L);
        assertThat(savedScore.getSalesCount()).isEqualTo(30L);
        assertThat(savedScore.getViewCount()).isEqualTo(4L);
        // 점수 = 6 * 0.3 + 30 * 0.5 + 4 * 0.2 = 1.8 + 15.0 + 0.8 = 17.6
        assertThat(savedScore.getScore()).isEqualTo(17.6);
    }

    @DisplayName("빈 Chunk는 처리하지 않는다")
    @Test
    void skipsEmptyChunk() throws Exception {
        // arrange
        Chunk<ProductMetrics> chunk = new Chunk<>(new ArrayList<>());

        // act
        writer.write(chunk);

        // assert
        verify(productRankScoreRepository, never()).findAllByProductIdIn(anySet());
        verify(productRankScoreRepository, never()).saveAll(anyList());
    }

    @DisplayName("여러 product_id를 가진 Chunk를 처리한다")
    @Test
    void processesMultipleProductIds() throws Exception {
        // arrange
        List<ProductMetrics> items = new ArrayList<>();
        
        for (long i = 1; i <= 5; i++) {
            ProductMetrics metrics = new ProductMetrics(i);
            metrics.incrementLikeCount();
            metrics.incrementSalesCount((int) i);
            metrics.incrementViewCount();
            items.add(metrics);
        }
        
        Chunk<ProductMetrics> chunk = new Chunk<>(items);
        
        when(productRankScoreRepository.findAllByProductIdIn(anySet())).thenReturn(List.of());
        doNothing().when(productRankScoreRepository).saveAll(anyList());

        // act
        writer.write(chunk);

        // assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductRankScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRankScoreRepository, times(1)).saveAll(captor.capture());
        
        List<ProductRankScore> savedScores = captor.getValue();
        assertThat(savedScores).hasSize(5);
        
        // 각 product_id별로 저장되었는지 확인
        for (long i = 1; i <= 5; i++) {
            long productId = i;
            ProductRankScore score = savedScores.stream()
                .filter(s -> s.getProductId().equals(productId))
                .findFirst()
                .orElseThrow();
            assertThat(score.getProductId()).isEqualTo(productId);
            assertThat(score.getLikeCount()).isEqualTo(1L);
            assertThat(score.getSalesCount()).isEqualTo(productId);
            assertThat(score.getViewCount()).isEqualTo(1L);
        }
    }

    @DisplayName("기존 데이터가 없으면 새로 생성한다")
    @Test
    void createsNewScoreWhenNoExistingData() throws Exception {
        // arrange
        List<ProductMetrics> items = new ArrayList<>();
        
        ProductMetrics metrics = new ProductMetrics(1L);
        metrics.incrementLikeCount();
        metrics.incrementSalesCount(10);
        metrics.incrementViewCount();
        items.add(metrics);
        
        Chunk<ProductMetrics> chunk = new Chunk<>(items);
        
        when(productRankScoreRepository.findAllByProductIdIn(anySet())).thenReturn(List.of());
        doNothing().when(productRankScoreRepository).saveAll(anyList());

        // act
        writer.write(chunk);

        // assert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ProductRankScore>> captor = ArgumentCaptor.forClass(List.class);
        verify(productRankScoreRepository, times(1)).saveAll(captor.capture());
        
        ProductRankScore savedScore = captor.getValue().get(0);
        assertThat(savedScore.getProductId()).isEqualTo(1L);
        assertThat(savedScore.getLikeCount()).isEqualTo(1L);
        assertThat(savedScore.getSalesCount()).isEqualTo(10L);
        assertThat(savedScore.getViewCount()).isEqualTo(1L);
    }
}

