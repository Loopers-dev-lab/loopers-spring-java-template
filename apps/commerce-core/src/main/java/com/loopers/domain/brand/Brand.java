                                                                                                                                                                                                                                                                             package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

@Entity
@Table(name = "tb_brand")
@Getter
public class Brand extends BaseEntity {
    private String name;

    protected Brand() {
    }

    private Brand(String name) {
        this.name = name;
    }

    public static Brand create(String name) {
        if (StringUtils.isBlank(name)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 이름은 필수이며 1자 이상이어야 합니다.");
        }
        return new Brand(name);
    }
}
