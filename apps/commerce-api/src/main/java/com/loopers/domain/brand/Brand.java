package com.loopers.domain.brand;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 브랜드 도메인 모델
 * - 순수 도메인 객체 (JPA 의존성 없음)
 * - 조회, 상태 확인 등 사용자 기능 중심
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Brand {

    private Long id;
    private String name;
    private String description;
    private String imageUrl;
    private BrandStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    /**
     * 재구성 팩토리 메서드 (Infrastructure에서 사용)
     */
    public static Brand reconstitute(
            Long id,
            String name,
            String description,
            String imageUrl,
            BrandStatus status,
            LocalDateTime createdAt,
            LocalDateTime modifiedAt
    ) {
        return new Brand(id, name, description, imageUrl, status, createdAt, modifiedAt);
    }

    /**
     * 조회 가능 여부 확인
     * - 삭제되지 않은 브랜드만 조회 가능
     */
    public boolean isViewable() {
        return this.status != BrandStatus.DELETED;
    }

    /**
     * 삭제된 브랜드인지 확인
     */
    public boolean isDeleted() {
        return this.status == BrandStatus.DELETED;
    }

    /**
     * 활성 상태인지 확인
     * - 정상적으로 운영 중인 브랜드
     */
    public boolean isActive() {
        return this.status == BrandStatus.ACTIVE;
    }

    /**
     * 중단된 브랜드인지 확인
     */
    public boolean isSuspended() {
        return this.status == BrandStatus.SUSPENDED;
    }
}
