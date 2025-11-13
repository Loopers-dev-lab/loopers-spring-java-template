package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * 포인트 Value Object.
 * <p>
 * 사용자의 포인트 잔액을 나타내는 불변 값 객체입니다.
 * 값으로 식별되며 불변성을 가집니다.
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Embeddable
@Getter
@EqualsAndHashCode
public class Point {
    @Column(name = "balance", nullable = false)
    private Long value;

    protected Point() {
        this.value = 0L; // JPA를 위한 기본 생성자
    }

    /**
     * Point 인스턴스를 생성합니다.
     *
     * @param value 포인트 값 (0 이상이어야 함)
     * @throws CoreException value가 null이거나 음수일 경우
     */
    public Point(Long value) {
        validateValue(value);
        this.value = value;
    }

    /**
     * Point 인스턴스를 생성하는 정적 팩토리 메서드.
     *
     * @param value 포인트 값
     * @return 생성된 Point 인스턴스
     * @throws CoreException value가 null이거나 음수일 경우
     */
    public static Point of(Long value) {
        return new Point(value);
    }

    /**
     * 포인트를 더한 새로운 Point 인스턴스를 반환합니다.
     *
     * @param other 더할 포인트
     * @return 새로운 Point 인스턴스
     * @throws CoreException other가 null일 경우
     */
    public Point add(Point other) {
        if (other == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트는 null일 수 없습니다.");
        }
        return new Point(this.value + other.value);
    }

    /**
     * 포인트를 뺀 새로운 Point 인스턴스를 반환합니다.
     * 포인트는 감소만 가능하며 음수가 되지 않도록 도메인 레벨에서 검증합니다.
     *
     * @param other 뺄 포인트
     * @return 새로운 Point 인스턴스
     * @throws CoreException other가 null이거나 잔액이 부족할 경우
     */
    public Point subtract(Point other) {
        if (other == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트는 null일 수 없습니다.");
        }
        if (this.value < other.value) {
            throw new CoreException(ErrorType.BAD_REQUEST,
                String.format("포인트가 부족합니다. (현재 잔액: %d, 요청 금액: %d)", this.value, other.value));
        }
        return new Point(this.value - other.value);
    }

    /**
     * 포인트 값의 유효성을 검증합니다.
     *
     * @param value 검증할 포인트 값
     * @throws CoreException value가 null이거나 음수일 경우
     */
    private void validateValue(Long value) {
        if (value == null || value < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트는 0 이상이어야 합니다.");
        }
    }
}

