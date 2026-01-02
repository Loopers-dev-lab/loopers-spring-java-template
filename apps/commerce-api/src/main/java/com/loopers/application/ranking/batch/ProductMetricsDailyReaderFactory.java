package com.loopers.application.ranking.batch;

import com.loopers.domain.metrics.product.ProductMetricsDailyAggregated;
import com.loopers.domain.metrics.product.ProductMetricsDailyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.AbstractPagingItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductMetricsDailyReaderFactory {
    private final ProductMetricsDailyRepository repository;

    @Bean
    @StepScope
    public AbstractPagingItemReader<ProductMetricsDailyAggregated> productMetricsDailyReader(
            @Value("#{jobParameters['periodType'] ?: 'weekly'}") String periodType,
            @Value("#{jobParameters['startDate'] ?: null}") String startDateStr,
            @Value("#{jobParameters['endDate'] ?: null}") String endDateStr,
            @Value("#{jobParameters['pageSize'] ?: 1000}") int pageSize
    ) {
        LocalDate startDate;
        LocalDate endDate;
        
        // periodType에 따라 날짜 범위 자동 계산
        // monthly는 최근 30일, weekly는 최근 7일로 자동 계산
        endDate = LocalDate.now();
        if ("monthly".equals(periodType)) {
            startDate = endDate.minusDays(30); // 최근 30일
            log.info("[ProductMetricsDailyReaderFactory] Auto-calculated date range for monthly: startDate={}, endDate={}", startDate, endDate);
        } else {
            // weekly: Job 파라미터에서 날짜가 제공된 경우 사용, 없으면 최근 7일
            if (startDateStr != null && endDateStr != null) {
                startDate = LocalDate.parse(startDateStr);
                endDate = LocalDate.parse(endDateStr);
                log.info("[ProductMetricsDailyReaderFactory] Using provided date range for weekly: startDate={}, endDate={}", startDate, endDate);
            } else {
                startDate = endDate.minusDays(7); // 최근 7일
                log.info("[ProductMetricsDailyReaderFactory] Auto-calculated date range for weekly: startDate={}, endDate={}", startDate, endDate);
            }
        }

        // Reader 생성 및 설정
        ProductMetricsDailyReader reader = new ProductMetricsDailyReader(repository);
        reader.setDateRange(startDate, endDate);
        reader.setPageSize(pageSize);
        reader.setSaveState(true); // 재시작을 위한 상태 저장
        reader.setName("productMetricsDailyReader");

        return reader;
    }

    @RequiredArgsConstructor
    public static class ProductMetricsDailyReader extends AbstractPagingItemReader<ProductMetricsDailyAggregated> {

        private final ProductMetricsDailyRepository repository;

        private LocalDate startDate;
        private LocalDate endDate;

        /**
         * 날짜 범위 설정
         * StepScope Bean으로 생성될 때 Job 파라미터에서 주입받음
         */
        public void setDateRange(LocalDate startDate, LocalDate endDate) {
            this.startDate = startDate;
            this.endDate = endDate;
        }

        /**
         * 한 페이지의 데이터를 로드하여 results에 저장
         * AbstractPagingItemReader가 자동으로 호출
         */
        @Override
        protected void doReadPage() {
            if (startDate == null || endDate == null) {
                throw new IllegalStateException("Date range must be set before reading. Use setDateRange() method.");
            }

            // 현재 페이지 번호는 부모 클래스의 page 필드에서 가져옴
            Pageable pageable = PageRequest.of(getPage(), getPageSize());

            // Repository에서 페이징 조회
            Page<ProductMetricsDailyAggregated> page =
                    repository.findAggregatedByDateBetweenPaged(startDate, endDate, pageable);

            // 결과를 results에 저장 (부모 클래스의 필드)
            if (results == null) {
                results = new CopyOnWriteArrayList<>();
            } else {
                results.clear();
            }

            results.addAll(page.getContent());

            log.info("Loaded page {}: {} items (total: {})",
                    getPage(), page.getContent().size(), page.getTotalElements());

            // 더 이상 읽을 페이지가 없으면 종료
            if (!page.hasNext()) {
                log.info("No more pages to read. Total items loaded: {}", page.getTotalElements());
            }
        }

        /**
         * Reader 초기화
         * Step 시작 시 호출됨
         */
        @Override
        protected void doOpen() throws Exception {
            super.doOpen();
            log.info("ProductMetricsDailyReader opened: startDate={}, endDate={}", startDate, endDate);
        }

        /**
         * Reader 종료
         * Step 종료 시 호출됨
         */
        @Override
        protected void doClose() throws Exception {
            super.doClose();
            log.info("ProductMetricsDailyReader closed");
        }
    }
}
