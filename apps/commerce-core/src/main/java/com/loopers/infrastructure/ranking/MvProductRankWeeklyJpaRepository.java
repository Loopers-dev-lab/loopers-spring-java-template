package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankWeekly;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface MvProductRankWeeklyJpaRepository extends JpaRepository<MvProductRankWeekly, Long> {
    List<MvProductRankWeekly> findTop100ByRankingDateOrderByRankAsc(ZonedDateTime rankingDate);

    Optional<MvProductRankWeekly> findByProductIdAndRankingDate(Long productId, ZonedDateTime rankingDate);

    @Modifying
    @Query("DELETE FROM MvProductRankWeekly r WHERE r.rankingDate = :rankingDate")
    void deleteByRankingDate(@Param("rankingDate") ZonedDateTime rankingDate);
}
