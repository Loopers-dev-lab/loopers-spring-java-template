package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankMonthly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface MvProductRankMonthlyJpaRepository extends JpaRepository<MvProductRankMonthly, Long> {
    List<MvProductRankMonthly> findTop100ByRankingDateOrderByRankAsc(ZonedDateTime rankingDate);

    Optional<MvProductRankMonthly> findByProductIdAndRankingDate(Long productId, ZonedDateTime rankingDate);

    @Modifying
    @Query("DELETE FROM MvProductRankMonthly r WHERE r.rankingDate = :rankingDate")
    void deleteByRankingDate(@Param("rankingDate") ZonedDateTime rankingDate);
}
