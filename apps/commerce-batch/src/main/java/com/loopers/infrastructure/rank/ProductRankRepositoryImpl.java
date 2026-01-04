package com.loopers.infrastructure.rank;

import com.loopers.domain.rank.ProductRank;
import com.loopers.domain.rank.ProductRankRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ProductRank Repository 구현체.
 * <p>
 * Materialized View에 저장된 상품 랭킹 데이터를 관리합니다.
 * </p>
 */
@Slf4j
@Repository
public class ProductRankRepositoryImpl implements ProductRankRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void saveRanks(ProductRank.PeriodType periodType, LocalDate periodStartDate, List<ProductRank> ranks) {
        // 기존 데이터 삭제
        deleteByPeriod(periodType, periodStartDate);

        // 새 데이터 저장
        for (ProductRank rank : ranks) {
            entityManager.persist(rank);
        }

        log.info("ProductRank 저장 완료: periodType={}, periodStartDate={}, count={}",
            periodType, periodStartDate, ranks.size());
    }

    @Override
    public List<ProductRank> findByPeriod(ProductRank.PeriodType periodType, LocalDate periodStartDate, int limit) {
        String jpql = "SELECT pr FROM ProductRank pr " +
                      "WHERE pr.periodType = :periodType AND pr.periodStartDate = :periodStartDate " +
                      "ORDER BY pr.rank ASC";

        return entityManager.createQuery(jpql, ProductRank.class)
            .setParameter("periodType", periodType)
            .setParameter("periodStartDate", periodStartDate)
            .setMaxResults(limit)
            .getResultList();
    }

    @Override
    public Optional<ProductRank> findByPeriodAndProductId(
        ProductRank.PeriodType periodType,
        LocalDate periodStartDate,
        Long productId
    ) {
        String jpql = "SELECT pr FROM ProductRank pr " +
                      "WHERE pr.periodType = :periodType " +
                      "AND pr.periodStartDate = :periodStartDate " +
                      "AND pr.productId = :productId";

        try {
            ProductRank rank = entityManager.createQuery(jpql, ProductRank.class)
                .setParameter("periodType", periodType)
                .setParameter("periodStartDate", periodStartDate)
                .setParameter("productId", productId)
                .getSingleResult();
            return Optional.of(rank);
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void deleteByPeriod(ProductRank.PeriodType periodType, LocalDate periodStartDate) {
        String jpql = "DELETE FROM ProductRank pr " +
                      "WHERE pr.periodType = :periodType AND pr.periodStartDate = :periodStartDate";

        int deletedCount = entityManager.createQuery(jpql)
            .setParameter("periodType", periodType)
            .setParameter("periodStartDate", periodStartDate)
            .executeUpdate();

        log.debug("ProductRank 삭제 완료: periodType={}, periodStartDate={}, deletedCount={}",
            periodType, periodStartDate, deletedCount);
    }
}

