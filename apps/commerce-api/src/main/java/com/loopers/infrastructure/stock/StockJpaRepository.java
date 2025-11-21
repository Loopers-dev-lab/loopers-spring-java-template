package com.loopers.infrastructure.stock;

import com.loopers.domain.stock.Stock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface StockJpaRepository extends JpaRepository<Stock, Long> {
  Optional<Stock> findByRefProductId(Long productId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select s from Stock s where s.refProductId = :id")
  Optional<Stock> findByProductIdForUpdate(@Param("id") Long id);
}
