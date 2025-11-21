package com.loopers.domain.brand;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;

/**
 * packageName : com.loopers.domain.brand
 * fileName     : Brand
 * author      : byeonsungmun
 * date        : 2025. 11. 11.
 * description :
 * ===========================================
 * DATE         AUTHOR       NOTE
 * -------------------------------------------
 * 2025. 11. 11.     byeonsungmun       최초 생성
 */
@Entity
@Table(name = "brand")
@Getter
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    protected  Brand() {}

    private Brand(String name) {
        this.name = requireValidName(name);
    }

    public static Brand create(String name) {
        return new Brand(name);
    }


    private String requireValidName(String name) {
        if (name == null || name.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품명 비어 있을수 없습니다.");
        }
        return name.trim();
    }
}
