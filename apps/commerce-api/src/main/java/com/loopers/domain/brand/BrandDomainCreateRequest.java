package com.loopers.domain.brand;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 브랜드 도메인 생성 요청 DTO
 * <p>
 * 도메인 레이어에서 사용하는 브랜드 생성 요청 정보입니다.
 * Application Layer에서 Domain Layer로 전달되는 데이터 구조입니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 12.
 */
public record BrandDomainCreateRequest(
        @NotBlank(message = "브랜드 이름은 필수입니다.")
        @Size(max = 100, message = "브랜드 이름은 100자를 초과할 수 없습니다.")
        String name,

        String description
) {
    /**
     * 정적 팩토리 메서드
     *
     * @param name        브랜드 이름
     * @param description 브랜드 설명
     * @return BrandDomainCreateRequest 인스턴스
     */
    public static BrandDomainCreateRequest of(String name, String description) {
        return new BrandDomainCreateRequest(name, description);
    }
}

