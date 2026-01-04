package com.loopers.infrastructure.rank;

import com.loopers.domain.rank.ProductRank;
import com.loopers.domain.rank.ProductRankRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ProductRank Repository 구현체.
 * <p>
 * Materialized View에 저장된 상품 랭킹 데이터를 조회합니다.
 * </p>
 */
@Slf4j
@Repository
@Transactional(readOnly = true)
public class ProductRankRepositoryImpl implements ProductRankRepository {

    @PersistenceContext
    private EntityManager entityManager;

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
}

