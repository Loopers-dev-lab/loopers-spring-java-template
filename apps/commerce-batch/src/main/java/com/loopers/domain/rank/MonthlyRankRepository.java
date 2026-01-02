package com.loopers.domain.rank;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface MonthlyRankRepository extends JpaRepository<MonthlyProductRank, Long> {

	List<MonthlyProductRank> findByPeriodStartOrderByRankPositionAsc(
		LocalDate periodStart, Pageable pageable
	);

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("DELETE FROM MonthlyProductRank m WHERE m.periodStart = :periodStart")
	int deleteByPeriodStart(@Param("periodStart") LocalDate periodStart);
}
