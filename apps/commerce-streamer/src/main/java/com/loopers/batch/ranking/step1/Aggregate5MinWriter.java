package com.loopers.batch.ranking.step1;

import com.loopers.batch.ranking.dto.ProductScore5MinDto;
import com.loopers.domain.ranking.ProductScore5Min;
import com.loopers.infrastructure.ranking.ProductScore5MinJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Step 1 Writer: ProductScore5Min 저장
 * 중복 제거 및 INSERT/UPDATE 분리 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class Aggregate5MinWriter implements ItemWriter<ProductScore5MinDto> {

    private final ProductScore5MinJpaRepository productScore5MinJpaRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    private static final int BATCH_SIZE = 500;

    @Override
    @Transactional
    public void write(Chunk<? extends ProductScore5MinDto> chunk) throws Exception {
        List<ProductScore5MinDto> items = new ArrayList<>(chunk.getItems());
        
        if (items.isEmpty()) {
            return;
        }

        // 1. 같은 (productId, startTime, endTime) 조합으로 그룹화하여 집계
        Map<String, ProductScore5MinDto> aggregated = new HashMap<>();
        
        for (ProductScore5MinDto dto : items) {
            String key = createKey(dto.getProductId(), dto.getStartTime(), dto.getEndTime());
            
            ProductScore5MinDto existing = aggregated.computeIfAbsent(key, k -> 
                ProductScore5MinDto.builder()
                    .productId(dto.getProductId())
                    .startTime(dto.getStartTime())
                    .endTime(dto.getEndTime())
                    .orderAmountSum(BigDecimal.ZERO)
                    .likeCount(0L)
                    .viewCount(0L)
                    .build()
            );
            
            // Raw Metrics 집계
            existing.setOrderAmountSum(existing.getOrderAmountSum().add(dto.getOrderAmountSum()));
            existing.setLikeCount(existing.getLikeCount() + dto.getLikeCount());
            existing.setViewCount(existing.getViewCount() + dto.getViewCount());
        }

        List<ProductScore5MinDto> aggregatedList = new ArrayList<>(aggregated.values());
        log.debug("Aggregated {} items into {} unique 5-minute intervals", 
            items.size(), aggregatedList.size());

        // 2. 배치 단위로 처리 (500건씩)
        for (int i = 0; i < aggregatedList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, aggregatedList.size());
            List<ProductScore5MinDto> batch = aggregatedList.subList(i, end);
            
            processBatch(batch);
        }
    }

    private void processBatch(List<ProductScore5MinDto> batch) {
        // 3. 기존 데이터 조회 (productId, startTime, endTime) 조합으로
        List<ProductScore5Min> existingEntities = batch.stream()
            .map(dto -> productScore5MinJpaRepository
                .findByProductIdAndTimeRange(dto.getProductId(), dto.getStartTime(), dto.getEndTime()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toList());

        Set<String> existingKeys = existingEntities.stream()
            .map(e -> createKey(e.getProductId(), e.getStartTime(), e.getEndTime()))
            .collect(Collectors.toSet());

        // 4. INSERT/UPDATE 분리
        List<ProductScore5Min> toInsert = new ArrayList<>();
        List<ProductScore5MinDto> toUpdate = new ArrayList<>();

        for (ProductScore5MinDto dto : batch) {
            String key = createKey(dto.getProductId(), dto.getStartTime(), dto.getEndTime());
            
            ProductScore5Min entity = ProductScore5Min.builder()
                .productId(dto.getProductId())
                .startTime(dto.getStartTime())
                .endTime(dto.getEndTime())
                .orderAmountSum(dto.getOrderAmountSum())
                .likeCount(dto.getLikeCount())
                .viewCount(dto.getViewCount())
                .build();

            if (existingKeys.contains(key)) {
                toUpdate.add(dto);
            } else {
                toInsert.add(entity);
            }
        }

        // 5. 배치 처리
        if (!toInsert.isEmpty()) {
            productScore5MinJpaRepository.saveAll(toInsert);
            log.debug("Inserted {} ProductScore5Min records", toInsert.size());
        }

        // 6. 배치 UPDATE 처리 (500건씩)
        if (!toUpdate.isEmpty()) {
            batchUpdateProductScore5Min(toUpdate);
            log.debug("Updated {} ProductScore5Min records", toUpdate.size());
        }
    }

    /**
     * 배치 UPDATE 처리 (500건씩)
     * EntityManager를 사용하여 실제 배치로 처리
     * 네이티브 쿼리는 Hibernate 배치와 작동하지 않으므로,
     * 개별 쿼리를 실행하되 EntityManager의 flush를 제어하여 성능 최적화
     */
    private void batchUpdateProductScore5Min(List<ProductScore5MinDto> updateList) {
        if (updateList.isEmpty()) {
            return;
        }

        // 배치 단위로 처리 (500건씩)
        for (int i = 0; i < updateList.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, updateList.size());
            List<ProductScore5MinDto> batch = updateList.subList(i, end);
            
            int totalUpdated = 0;
            // 개별 업데이트를 실행하되, 배치 단위로 flush
            for (ProductScore5MinDto dto : batch) {
                int updated = productScore5MinJpaRepository.updateProductScore5MinForBatch(
                    dto.getProductId(),
                    dto.getStartTime(),
                    dto.getEndTime(),
                    dto.getOrderAmountSum(),
                    dto.getLikeCount(),
                    dto.getViewCount()
                );
                totalUpdated += updated;
            }
            
            // 배치 단위로 flush (트랜잭션 커밋 전에 모든 업데이트를 한 번에 실행)
            entityManager.flush();
            
            log.debug("Batch updated {} ProductScore5Min records ({} attempted)", totalUpdated, batch.size());
        }
    }

    /**
     * (productId, startTime, endTime) 조합을 키로 변환
     */
    private String createKey(Long productId, LocalDateTime startTime, LocalDateTime endTime) {
        return String.format("%d_%s_%s", productId, startTime, endTime);
    }
}

