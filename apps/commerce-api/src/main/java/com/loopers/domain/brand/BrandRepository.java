package com.loopers.domain.brand;

import java.util.List;
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
 * Retrieves the Brand with the given ID.
 *
 * @param brandId the primary key of the Brand to retrieve
 * @return an Optional containing the Brand if found, otherwise an empty Optional
 */
    Optional<Brand> findById(Long brandId);

    /**
 * Retrieve Brand entities for the given list of IDs using a batch query to avoid N+1 query issues.
 *
 * @param brandIds the list of brand IDs to fetch
 * @return a list of Brands matching the provided IDs; IDs not found are omitted from the result
 */
    List<Brand> findAllById(List<Long> brandIds);
}
