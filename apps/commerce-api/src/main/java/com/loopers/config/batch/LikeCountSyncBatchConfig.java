package com.loopers.config.batch;

import com.loopers.domain.like.LikeRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Map;

/**
 * 좋아요 수 동기화 배치 Job Configuration.
 * <p>
 * Spring Batch를 사용하여 Like 테이블의 COUNT(*) 결과를 Product.likeCount 필드에 동기화합니다.
 * </p>
 * <p>
 * <b>배치 구조:</b>
 * <ol>
 *   <li><b>Reader:</b> 모든 상품 ID 조회</li>
 *   <li><b>Processor:</b> 각 상품의 좋아요 수 집계 (Like 테이블 COUNT(*))</li>
 *   <li><b>Writer:</b> Product.likeCount 필드 업데이트</li>
 * </ol>
 * </p>
 * <p>
 * <b>설계 근거:</b>
 * <ul>
 *   <li><b>대량 처리:</b> Spring Batch의 청크 단위 처리로 성능 최적화</li>
 *   <li><b>트랜잭션 관리:</b> 청크 단위로 커밋하여 안정성 보장</li>
 *   <li><b>재시작 가능:</b> Job 실패 시 재시작 가능</li>
 *   <li><b>모니터링:</b> Spring Batch 메타데이터로 실행 이력 추적</li>
 * </ul>
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class LikeCountSyncBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final ProductRepository productRepository;
    private final LikeRepository likeRepository;

    private static final int CHUNK_SIZE = 100; // 청크 크기: 100개씩 처리

    /**
     * Creates the batch job that synchronizes product like counts.
     *
     * @return the Job configured to execute the like-count synchronization step
     */
    @Bean
    public Job likeCountSyncJob() {
        return new JobBuilder("likeCountSyncJob", jobRepository)
            .start(likeCountSyncStep())
            .build();
    }

    /**
     * Creates the batch Step that synchronizes product like counts.
     *
     * @return the Step configured to read product IDs, compute their like counts, and update products
     */
    @Bean
    public Step likeCountSyncStep() {
        return new StepBuilder("likeCountSyncStep", jobRepository)
            .<Long, ProductLikeCount>chunk(CHUNK_SIZE, transactionManager)
            .reader(productIdReader())
            .processor(productLikeCountProcessor())
            .writer(productLikeCountWriter())
            .build();
    }

    /**
     * Reads all product IDs for like-count synchronization.
     *
     * @return an ItemReader that provides product IDs to be processed
     */
    @Bean
    public ItemReader<Long> productIdReader() {
        List<Long> productIds = productRepository.findAllProductIds();
        log.debug("좋아요 수 동기화 대상 상품 수: {}", productIds.size());
        return new ListItemReader<>(productIds);
    }

    /**
     * Creates an ItemProcessor that computes the total like count for a given product ID.
     *
     * The processor produces a ProductLikeCount containing the original productId and its like count (0 if no likes are found).
     *
     * @return an ItemProcessor that maps a product ID to a ProductLikeCount with the product's like count
     */
    @Bean
    public ItemProcessor<Long, ProductLikeCount> productLikeCountProcessor() {
        return productId -> {
            // Like 테이블에서 해당 상품의 좋아요 수 집계
            Map<Long, Long> likeCountMap = likeRepository.countByProductIds(List.of(productId));
            Long likeCount = likeCountMap.getOrDefault(productId, 0L);
            return new ProductLikeCount(productId, likeCount);
        };
    }

    /**
     * Create an ItemWriter that updates Product.likeCount in the database for each provided item.
     *
     * @return an ItemWriter that applies each item's like count to the corresponding product; if an update for an individual item fails, the failure is logged and remaining items are processed
     */
    @Bean
    public ItemWriter<ProductLikeCount> productLikeCountWriter() {
        return items -> {
            for (ProductLikeCount item : items) {
                try {
                    productRepository.updateLikeCount(item.productId(), item.likeCount());
                } catch (Exception e) {
                    log.warn("상품 좋아요 수 업데이트 실패: productId={}, likeCount={}, error={}",
                        item.productId(), item.likeCount(), e.getMessage());
                    // 개별 실패는 로그만 남기고 계속 진행
                }
            }
        };
    }

    /**
     * 상품 ID와 좋아요 수를 담는 레코드.
     *
     * @param productId 상품 ID
     * @param likeCount 좋아요 수
     */
    public record ProductLikeCount(Long productId, Long likeCount) {
    }
}
