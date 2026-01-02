package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankWeekly;
import com.loopers.domain.ranking.ProductRankId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MvProductRankWeeklyJpaRepository extends JpaRepository<MvProductRankWeekly, ProductRankId> {

    List<MvProductRankWeekly> findByIdPeriodKeyOrderByRankAsc(String periodKey);

    void deleteByIdPeriodKey(String periodKey);
}
