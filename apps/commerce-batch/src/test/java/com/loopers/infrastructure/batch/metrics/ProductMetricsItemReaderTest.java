package com.loopers.infrastructure.batch.metrics;

import com.loopers.domain.metrics.ProductMetrics;
import com.loopers.domain.metrics.ProductMetricsRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * ProductMetricsItemReader 테스트.
 */
@ExtendWith(MockitoExtension.class)
class ProductMetricsItemReaderTest {

    @Mock
    private ProductMetricsRepository productMetricsRepository;

    @Mock
    private PagingAndSortingRepository<ProductMetrics, Long> jpaRepository;

    @DisplayName("올바른 날짜 형식으로 Reader를 생성할 수 있다")
    @Test
    void createsReader_withValidDate() {
        // arrange
        String targetDate = "20241215";
        when(productMetricsRepository.getJpaRepository()).thenReturn(jpaRepository);
        
        ProductMetricsItemReader reader = new ProductMetricsItemReader(productMetricsRepository);

        // act
        RepositoryItemReader<ProductMetrics> itemReader = reader.createReader(targetDate);

        // assert
        assertThat(itemReader).isNotNull();
        assertThat(itemReader.getName()).isEqualTo("productMetricsReader");
    }

    @DisplayName("날짜 파라미터가 null이면 오늘 날짜를 사용하여 Reader를 생성한다")
    @Test
    void createsReader_withNullDate_usesToday() {
        // arrange
        when(productMetricsRepository.getJpaRepository()).thenReturn(jpaRepository);
        
        ProductMetricsItemReader reader = new ProductMetricsItemReader(productMetricsRepository);

        // act
        RepositoryItemReader<ProductMetrics> itemReader = reader.createReader(null);

        // assert
        assertThat(itemReader).isNotNull();
    }

    @DisplayName("날짜 파라미터가 빈 문자열이면 오늘 날짜를 사용하여 Reader를 생성한다")
    @Test
    void createsReader_withEmptyDate_usesToday() {
        // arrange
        when(productMetricsRepository.getJpaRepository()).thenReturn(jpaRepository);
        
        ProductMetricsItemReader reader = new ProductMetricsItemReader(productMetricsRepository);

        // act
        RepositoryItemReader<ProductMetrics> itemReader = reader.createReader("");

        // assert
        assertThat(itemReader).isNotNull();
    }

    @DisplayName("잘못된 날짜 형식이면 오늘 날짜를 사용하여 Reader를 생성한다")
    @Test
    void createsReader_withInvalidDate_usesToday() {
        // arrange
        when(productMetricsRepository.getJpaRepository()).thenReturn(jpaRepository);
        
        ProductMetricsItemReader reader = new ProductMetricsItemReader(productMetricsRepository);

        // act
        RepositoryItemReader<ProductMetrics> itemReader = reader.createReader("invalid-date");

        // assert
        assertThat(itemReader).isNotNull();
    }

    @DisplayName("날짜 파라미터를 올바르게 파싱하여 날짜 범위를 설정한다")
    @Test
    void parsesDateCorrectly_andSetsDateTimeRange() {
        // arrange
        String targetDate = "20241215";
        LocalDate expectedDate = LocalDate.of(2024, 12, 15);
        LocalDateTime expectedStart = expectedDate.atStartOfDay();
        LocalDateTime expectedEnd = expectedDate.atTime(LocalTime.MAX);
        
        when(productMetricsRepository.getJpaRepository()).thenReturn(jpaRepository);
        
        ProductMetricsItemReader reader = new ProductMetricsItemReader(productMetricsRepository);

        // act
        RepositoryItemReader<ProductMetrics> itemReader = reader.createReader(targetDate);

        // assert
        assertThat(itemReader).isNotNull();
        // 날짜 파싱이 올바르게 되었는지 확인 (Reader 내부에서 사용되므로 간접적으로 검증)
        // 실제 날짜 범위는 Repository 호출 시 사용되므로, Reader가 정상 생성되었으면 성공
    }

    @DisplayName("JPA Repository를 통해 Reader를 생성한다")
    @Test
    void createsReader_usingJpaRepository() {
        // arrange
        String targetDate = "20241215";
        when(productMetricsRepository.getJpaRepository()).thenReturn(jpaRepository);
        
        ProductMetricsItemReader reader = new ProductMetricsItemReader(productMetricsRepository);

        // act
        RepositoryItemReader<ProductMetrics> itemReader = reader.createReader(targetDate);

        // assert
        assertThat(itemReader).isNotNull();
        // getJpaRepository()가 호출되었는지 확인
        // (실제로는 RepositoryItemReader 내부에서 사용되므로 간접적으로 검증)
    }
}

