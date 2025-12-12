package com.loopers.interfaces.api.brand;

import com.loopers.application.brand.BrandInfo;
import org.springframework.data.domain.Page;

import java.util.List;

public class BrandV1Dto {
    
    public record BrandCreateRequest(
            String name
    ) {
    }
    
    public record BrandResponse(
            Long id,
            String name
    ) {
        public static BrandResponse from(BrandInfo info) {
            return new BrandResponse(
                    info.id(),
                    info.name()
            );
        }
        
        public static List<BrandResponse> fromList(List<BrandInfo> infos) {
            return infos.stream()
                    .map(BrandResponse::from)
                    .toList();
        }
    }
    
    public record BrandsPageResponse(
            List<BrandResponse> content,
            int totalPages,
            long totalElements,
            int number,
            int size
    ) {
        public static BrandsPageResponse from(Page<BrandInfo> page) {
            return new BrandsPageResponse(
                    page.map(BrandResponse::from).getContent(),
                    page.getTotalPages(),
                    page.getTotalElements(),
                    page.getNumber(),
                    page.getNumberOfElements()
            );
        }
    }
    
    public record BrandBulkCreateRequest(
            List<String> names
    ) {
    }
    
    public record BrandBulkCreateResponse(
            List<BrandResponse> brands
    ) {
        public static BrandBulkCreateResponse from(List<BrandInfo> infos) {
            return new BrandBulkCreateResponse(
                    BrandResponse.fromList(infos)
            );
        }
    }
}


