package com.loopers.infrastructure.rank;

import com.loopers.domain.rank.ProductRankScore;
import com.loopers.domain.rank.ProductRankScoreRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * ProductRankScore Repository 구현체.
 * <p>
 * Step 1과 Step 2 간 데이터 전달을 위한 임시 테이블을 관리합니다.
 * </p>
 */
@Slf4j
@Repository
public class ProductRankScoreRepositoryImpl implements ProductRankScoreRepository {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional
    public void save(ProductRankScore score) {
        Optional<ProductRankScore> existing = findByProductId(score.getProductId());
        
        if (existing.isPresent()) {
            // 기존 레코드가 있으면 덮어쓰기 (Writer에서 이미 누적된 값을 전달받음)
            ProductRankScore existingScore = existing.get();
            existingScore.setMetrics(
                score.getLikeCount(),
                score.getSalesCount(),
                score.getViewCount(),
                score.getScore()
            );
            entityManager.merge(existingScore);
            log.debug("ProductRankScore 업데이트: productId={}", score.getProductId());
        } else {
            // 없으면 새로 생성
            entityManager.persist(score);
            log.debug("ProductRankScore 생성: productId={}", score.getProductId());
        }
    }

    @Override
    @Transactional
    public void saveAll(List<ProductRankScore> scores) {
        for (ProductRankScore score : scores) {
            save(score);
        }
        log.info("ProductRankScore 일괄 저장 완료: count={}", scores.size());
    }

    @Override
    public Optional<ProductRankScore> findByProductId(Long productId) {
        String jpql = "SELECT prs FROM ProductRankScore prs WHERE prs.productId = :productId";
        
        try {
            ProductRankScore score = entityManager.createQuery(jpql, ProductRankScore.class)
                .setParameter("productId", productId)
                .getSingleResult();
            return Optional.of(score);
        } catch (jakarta.persistence.NoResultException e) {
            return Optional.empty();
        }
    }

    @Override
    public List<ProductRankScore> findAllByProductIdIn(Set<Long> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }
        
        String jpql = "SELECT prs FROM ProductRankScore prs WHERE prs.productId IN :productIds";
        return entityManager.createQuery(jpql, ProductRankScore.class)
            .setParameter("productIds", productIds)
            .getResultList();
    }

    @Override
    public List<ProductRankScore> findAllOrderByScoreDesc(int limit) {
        String jpql = "SELECT prs FROM ProductRankScore prs ORDER BY prs.score DESC";
        
        jakarta.persistence.TypedQuery<ProductRankScore> query = 
            entityManager.createQuery(jpql, ProductRankScore.class);
        if (limit > 0) {
            query.setMaxResults(limit);
        }
        
        return query.getResultList();
    }

    @Override
    public List<ProductRankScore> findAll() {
        String jpql = "SELECT prs FROM ProductRankScore prs";
        return entityManager.createQuery(jpql, ProductRankScore.class).getResultList();
    }

    @Override
    @Transactional
    public void deleteAll() {
        String jpql = "DELETE FROM ProductRankScore";
        int deletedCount = entityManager.createQuery(jpql).executeUpdate();
        log.info("ProductRankScore 전체 삭제 완료: deletedCount={}", deletedCount);
    }
}

