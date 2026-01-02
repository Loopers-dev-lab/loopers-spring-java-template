package com.loopers.infrastructure.ranking;

import com.loopers.domain.ranking.MvProductRankMonthly;
import com.loopers.domain.ranking.ProductRankId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MvProductRankMonthlyJpaRepository extends JpaRepository<MvProductRankMonthly, ProductRankId> {

    List<MvProductRankMonthly> findByIdPeriodKeyOrderByRankAsc(String periodKey);

    void deleteByIdPeriodKey(String periodKey);
}
