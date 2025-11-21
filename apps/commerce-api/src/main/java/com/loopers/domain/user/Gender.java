package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

@Embeddable
public class Gender {
    private String gender;

    protected Gender() {
    }

    public Gender(String gender) {
        //성별 체크
        if (gender == null || gender.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "성별은 비어있을 수 없습니다.");
        }
        this.gender = gender;
    }

    public String gender() {
        return gender;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Gender gender1 = (Gender) o;
        return gender != null ? gender.equals(gender1.gender) : gender1.gender == null;
    }

    @Override
    public int hashCode() {
        return gender != null ? gender.hashCode() : 0;
    }
}
