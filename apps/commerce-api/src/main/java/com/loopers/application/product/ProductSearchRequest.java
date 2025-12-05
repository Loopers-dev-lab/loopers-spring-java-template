package com.loopers.application.product;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;

public record ProductSearchRequest(
        Pageable pageable,
        Filter filter
) {
    public ProductSearchRequest(Pageable pageable) {
        this(pageable, new Filter(List.of()));
    }

    // 현재 편의를 위해 단건 brandId 생성자 추가
    public ProductSearchRequest(Pageable pageable, Long brandId) {
        this(pageable, new Filter(brandId != null ? List.of(brandId) : List.of()));
    }

    public ProductSearchRequest(Pageable pageable, List<Long> brandIds) {
        this(pageable, new Filter(brandIds));
    }

    public ProductSearchRequest(Pageable pageable, Filter filter) {
        this.pageable = pageable;
        this.filter = filter;
        validate();
    }

    public record Filter(
            List<Long> brandIds
//            List<Long> categoryIds
//            Long minPrice
//            Long maxPrice
//            String searchKeyword
    ) {
    }

    private void validate() {
        Sort sort = pageable().getSort();
        String sortStr = sort.toString();
        if (!(sort.isUnsorted()
                || sortStr.equals("likeCount: DESC")
                || sortStr.equals("price: ASC")
                || sortStr.equals("createdAt: DESC"))) {
            throw new IllegalArgumentException("유효하지 않은 정렬 기준입니다.");
        }
    }
}
