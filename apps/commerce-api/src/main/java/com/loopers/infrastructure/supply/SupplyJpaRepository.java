package com.loopers.infrastructure.supply;

import com.loopers.domain.supply.Supply;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SupplyJpaRepository extends JpaRepository<Supply, Long> {
    Optional<Supply> findByProductId(Long productId);

    List<Supply> findAllByProductIdIn(Collection<Long> productIds);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Supply s WHERE s.productId = :productId")
    Optional<Supply> findByProductIdForUpdate(Long productId);
}
