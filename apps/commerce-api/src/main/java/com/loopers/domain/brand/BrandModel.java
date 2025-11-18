package com.loopers.domain.brand;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "brand")
public class BrandModel extends BaseEntity {
    @Column(nullable = false,  length = 50)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BrandStatus status;

    protected BrandModel() {}

    public BrandModel(String name, String description, BrandStatus status) {
        validateBrandName(name);
        validateBrandStatus(status);

        this.name = name;
        this.description = description;
        this.status = status;
    }

    private void validateBrandName(String name) {
        if(name == null || name.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 이름은 필수 입력값 입니다.");
        }

        if(name.length() > 50) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 이름은 50자 이하 입력값 입니다.");
        }

        this.name = name;
    }

    /**
     * @param status
     */
    private void validateBrandStatus(BrandStatus status) {
        if(status == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상태값 필수 입력");
        }
        this.status = status;

    }

    public boolean isDiscontinued() {
        return this.status == BrandStatus.DISCONITNUED;
    }

    public boolean isRegistered() {
        return this.status == BrandStatus.REGISTERED;
    }

    public void setRegistered() {
        this.status = BrandStatus.REGISTERED;
    }

    public void setDiscontinued() {
        if(this.status == BrandStatus.DISCONITNUED) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이미 해지된 브랜드");
        }
        this.status = BrandStatus.DISCONITNUED;
    }

}
