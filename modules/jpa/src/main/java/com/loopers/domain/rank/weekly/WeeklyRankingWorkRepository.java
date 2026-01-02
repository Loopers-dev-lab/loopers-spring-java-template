package com.loopers.domain.rank.weekly;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WeeklyRankingWorkRepository extends JpaRepository<WeeklyRankingWork, Long> {

}
