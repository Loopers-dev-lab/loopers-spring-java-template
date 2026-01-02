package com.loopers.domain.rank.monthly;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MonthlyRankingMVRepository extends JpaRepository<MonthlyRankingMV, Long> {

  List<MonthlyRankingMV> findByBaseDateOrderByCurrentRankAsc(String baseDate, Pageable pageable);
}
