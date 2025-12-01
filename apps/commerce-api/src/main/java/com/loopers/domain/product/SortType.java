package com.loopers.domain.product;

import org.springframework.data.domain.Sort;

/**
 * packageName : com.loopers.domain.product
 * fileName     : SortType
 * author      : byeonsungmun
 * date        : 2025. 11. 27.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 27.     byeonsungmun       최초 생성
 */
public enum SortType {

    LIKES_DESC("likes_desc", Sort.by(Sort.Direction.DESC, "likeCount")),
    PRICE_ASC("price_asc", Sort.by(Sort.Direction.ASC, "price")),
    LATEST("latest", Sort.by(Sort.Direction.DESC, "createdAt"));

    private final String value;
    private final Sort sort;

    SortType(String value, Sort sort) {
        this.value = value;
        this.sort = sort;
    }

    public Sort toSort() {
        return this.sort;
    }

    public static SortType from(String value) {
        for (SortType type : SortType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        return LATEST;
    }

}
