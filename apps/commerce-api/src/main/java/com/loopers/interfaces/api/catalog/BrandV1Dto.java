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
         * BrandInfo로부터 BrandResponse를 생성합니다.
         *
         * @param brandInfo 브랜드 정보
         * @return 생성된 응답 객체
         */
        public static BrandResponse from(CatalogBrandFacade.BrandInfo brandInfo) {
            return new BrandResponse(brandInfo.id(), brandInfo.name());
        }
    }
}

