package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BrandJpaRepository extends JpaRepository<BrandModel, Long> {
    boolean existsByName(String brandName);
    Optional<BrandModel> findByName(String brandName);
    List<BrandModel> findAllByStatus(BrandStatus brandStatus);

    /**
     * bulk 용 쿼리
     * @param status
     * @param name
     * @return
     */
    @Modifying
    @Query("UPDATE brand b SET b.status = :status where b.name = :name")
    int updateStatusByName(@Param("status") BrandStatus status, @Param("name") String name);

    /**
     * bulk 용 쿼리
     * @param status
     * @param id
     * @return
     */
    @Modifying
    @Query("UPDATE brand b SET b.status = :status where b.id = :id")
    int updateStatusById(@Param("status") BrandStatus status, @Param("id") Long id);

}
