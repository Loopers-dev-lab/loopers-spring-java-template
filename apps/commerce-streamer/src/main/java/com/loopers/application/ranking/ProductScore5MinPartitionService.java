package com.loopers.application.ranking;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * ProductScore5Min 테이블 파티션 관리 서비스
 * 일일 기준으로 파티셔닝하고, 30일 이전 파티션을 자동으로 삭제
 */
@Slf4j
@Service
public class ProductScore5MinPartitionService {

    @PersistenceContext
    private EntityManager entityManager;

    private static final String TABLE_NAME = "product_score_5min";
    private static final DateTimeFormatter PARTITION_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter SQL_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 내일 날짜 파티션 생성 (매일 자정에 실행)
     * 파티션이 이미 존재하면 무시
     */
    @Transactional
    public void createNextDayPartition() {
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        String partitionName = createPartitionName(tomorrow);
        String partitionDate = tomorrow.format(SQL_DATE_FORMATTER);
        
        // TO_DAYS 함수를 사용하여 DATE 기준 파티셔닝
        String sql = String.format(
            "ALTER TABLE %s " +
            "ADD PARTITION (PARTITION %s VALUES LESS THAN (TO_DAYS('%s') + 1))",
            TABLE_NAME, partitionName, partitionDate
        );
        
        try {
            entityManager.createNativeQuery(sql).executeUpdate();
            log.info("Created partition: {} for date: {}", partitionName, partitionDate);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("Duplicate partition name")) {
                log.debug("Partition already exists: {}", partitionName);
            } else {
                log.error("Failed to create partition: {} for date: {}", partitionName, partitionDate, e);
                throw e;
            }
        }
    }

    /**
     * 30일 이전 파티션 삭제 (매일 자정에 실행)
     * 파티션이 존재하지 않으면 무시
     */
    @Transactional
    public void dropOldPartitions() {
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        String partitionName = createPartitionName(thirtyDaysAgo);
        
        // MySQL 8.0.23 이상에서는 IF EXISTS 지원, 그 이하 버전을 대비한 예외 처리
        String sql = String.format(
            "ALTER TABLE %s DROP PARTITION %s",
            TABLE_NAME, partitionName
        );
        
        try {
            entityManager.createNativeQuery(sql).executeUpdate();
            log.info("Dropped old partition: {}", partitionName);
        } catch (Exception e) {
            // 파티션이 존재하지 않는 경우는 정상적인 상황 (이미 삭제되었거나 처음 실행)
            String errorMessage = e.getMessage();
            if (errorMessage != null && (
                errorMessage.contains("does not exist") ||
                errorMessage.contains("Unknown partition") ||
                errorMessage.contains("Error Code: 1507")
            )) {
                log.debug("Partition does not exist or already dropped: {}", partitionName);
            } else {
                log.error("Failed to drop partition: {}", partitionName, e);
                throw e;
            }
        }
    }

    /**
     * 파티션 이름 생성
     */
    private String createPartitionName(LocalDate date) {
        return "p_" + date.format(PARTITION_DATE_FORMATTER);
    }
}

