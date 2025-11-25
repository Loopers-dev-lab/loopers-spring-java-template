package com.loopers.domain.stock;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "stock")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Stock extends BaseEntity {

    private Long quantity;

    @Column(unique = true)
    private Long productId;

    @Builder
    private Stock(
        Long quantity
        , Long productId
    ) {
        this.quantity = quantity;
        this.productId = productId;
        
        // 유효성 검사
        guard();
    }
    
    // 유효성 검사
    @Override
    protected void guard() {
        // quantity 유효성 검사
        if(quantity == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Stock : quantity 가 비어있을 수 없습니다.");
        } else if(quantity < 0L) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Stock : quantity 는 음수가 될 수 없습니다.");
        }

        // productId 유효성 검사
        if(productId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "Stock : productId 가 비어있을 수 없습니다.");
        }
    }

}
