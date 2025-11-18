package com.loopers.domain.like;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@NoArgsConstructor
@Getter
@Entity
@Table(
    name = "likes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"member_id", "product_id"})
)
public class Like extends BaseEntity {

    @Column(name = "member_id", nullable = false, length = 10)
    private String memberId;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    public Like(String memberId, Long productId) {
        validateMemberId(memberId);
        validateProductId(productId);
        this.memberId = memberId;
        this.productId = productId;
    }

    private static void validateMemberId(String memberId) {
        if (memberId == null || memberId.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "회원 ID는 필수입니다.");
        }
    }

    private static void validateProductId(Long productId) {
        if (productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 ID는 필수입니다.");
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Like like = (Like) o;
        return Objects.equals(memberId, like.memberId) && Objects.equals(productId, like.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, productId);
    }
}
