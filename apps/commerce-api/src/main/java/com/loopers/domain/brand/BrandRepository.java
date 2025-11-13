package com.loopers.domain.brand;

import java.util.Optional;

/**
 * Brand 엔티티에 대한 저장소 인터페이스.
 * <p>
 * 브랜드 정보의 영속성 계층과의 상호작용을 정의합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
public interface BrandRepository {
    /**
     * 브랜드를 저장합니다.
     *
     * @param brand 저장할 브랜드
     * @return 저장된 브랜드
     */
    Brand save(Brand brand);
    
    /**
     * 브랜드 ID로 브랜드를 조회합니다.
     *
     * @param brandId 조회할 브랜드 ID
     * @return 조회된 브랜드를 담은 Optional
     */
    Optional<Brand> findById(Long brandId);
}

