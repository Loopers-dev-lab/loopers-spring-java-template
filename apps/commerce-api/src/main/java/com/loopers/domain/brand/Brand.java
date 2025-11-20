package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 브랜드 도메인 엔티티.
 * <p>
 * 브랜드의 기본 정보(이름)를 관리합니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Entity
@Table(name = "brand")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Brand extends BaseEntity {
    @Column(name = "name", nullable = false)
    private String name;

    /**
     * Create a Brand with the given name, validating that the name is present.
     *
     * @param name the brand name
     * @throws CoreException if name is null or blank
     */
    public Brand(String name) {
        validateName(name);
        this.name = name;
    }

    /**
     * Validate that the brand name is present and not blank.
     *
     * @param name the brand name to validate
     * @throws CoreException if {@code name} is null or blank; thrown with {@link ErrorType#BAD_REQUEST}
     */
    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 이름은 필수입니다.");
        }
    }

    /**
     * Brand 인스턴스를 생성하는 정적 팩토리 메서드.
     *
     * @param name 브랜드 이름
     * @return 생성된 Brand 인스턴스
     */
    public static Brand of(String name) {
        return new Brand(name);
    }
}
