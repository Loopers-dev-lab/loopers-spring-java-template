package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * Product 엔티티를 위한 Spring Data JPA 리포지토리.
 * <p>
 * JpaRepository를 확장하여 기본 CRUD 기능을 제공합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public interface ProductJpaRepository extends JpaRepository<Product, Long> {
    /**
     * 브랜드 ID로 상품을 조회합니다.
     *
     * @param brandId 브랜드 ID
     * @param pageable 페이징 정보
     * @return 상품 페이지
     */
    Page<Product> findByBrandId(Long brandId, Pageable pageable);

    /**
     * 전체 상품을 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 상품 페이지
     */
    Page<Product> findAll(Pageable pageable);

    /**
     * 브랜드 ID로 상품 개수를 조회합니다.
     *
     * @param brandId 브랜드 ID
     * @return 상품 개수
     */
    long countByBrandId(Long brandId);

    /**
     * 상품 ID로 상품을 조회합니다. (비관적 락)
     * <p>
     * SELECT ... FOR UPDATE를 사용하여 동시성 제어를 보장합니다.
     * </p>
     * <p>
     * <b>Lock 전략:</b>
     * <ul>
     *   <li><b>PESSIMISTIC_WRITE 선택 근거:</b> 재고 차감 시 Lost Update 방지</li>
     *   <li><b>Lock 범위 최소화:</b> PK(id) 기반 조회로 해당 행만 락</li>
     *   <li><b>인덱스 활용:</b> PK는 자동으로 인덱스가 생성되어 Lock 범위 최소화</li>
     * </ul>
     * </p>
     * <p>
     * <b>동작 원리:</b>
     * <ol>
     *   <li>SELECT ... FOR UPDATE 실행 → 해당 행에 배타적 락 설정</li>
     *   <li>다른 트랜잭션의 쓰기/FOR UPDATE는 차단 (일반 읽기는 가능)</li>
     *   <li>재고 차감 후 트랜잭션 커밋 → 락 해제</li>
     *   <li>대기 중이던 트랜잭션이 최신 값을 읽어 처리</li>
     * </ol>
     * </p>
     *
     * @param productId 조회할 상품 ID
     * @return 조회된 상품을 담은 Optional
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :productId")
    Optional<Product> findByIdForUpdate(@Param("productId") Long productId);

    /**
     * 모든 상품 ID를 조회합니다.
     * <p>
     * 비동기 집계 스케줄러에서 사용됩니다.
     * </p>
     *
     * @return 모든 상품 ID 목록
     */
    @Query("SELECT p.id FROM Product p")
    List<Long> findAllProductIds();
}

