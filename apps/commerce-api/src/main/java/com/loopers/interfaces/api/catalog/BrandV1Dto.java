package com.loopers.interfaces.api.catalog;

import com.loopers.application.catalog.CatalogBrandFacade;

/**
 * 브랜드 조회 API v1의 데이터 전송 객체(DTO) 컨테이너.
 *
 * @author Loopers
 * @version 1.0
 */
public class BrandV1Dto {
    /**
     * 브랜드 정보 응답 데이터.
     *
     * @param brandId 브랜드 ID
     * @param name 브랜드 이름
     */
    public record BrandResponse(Long brandId, String name) {
        /**
         * Create a BrandResponse from a CatalogBrandFacade.BrandInfo.
         *
         * @param brandInfo the source brand information to map
         * @return a BrandResponse containing the brandId and name extracted from {@code brandInfo}
         */
        public static BrandResponse from(CatalogBrandFacade.BrandInfo brandInfo) {
            return new BrandResponse(brandInfo.id(), brandInfo.name());
        }
    }
}
