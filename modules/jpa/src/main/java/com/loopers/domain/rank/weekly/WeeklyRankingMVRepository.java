package com.loopers.domain.rank.weekly;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyRankingMVRepository extends JpaRepository<WeeklyRankingMV, Long> {

  List<WeeklyRankingMV> findByBaseDateOrderByCurrentRankAsc(String baseDate, Pageable pageable);
}
