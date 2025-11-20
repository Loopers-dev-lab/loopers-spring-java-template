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
     * Brand 인스턴스를 생성합니다.
     *
     * @param name 브랜드 이름
     * @throws CoreException name이 null이거나 공백일 경우
     */
    public Brand(String name) {
        validateName(name);
        this.name = name;
    }

    /**
     * 브랜드 이름의 유효성을 검증합니다.
     *
     * @param name 검증할 브랜드 이름
     * @throws CoreException name이 null이거나 공백일 경우
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

