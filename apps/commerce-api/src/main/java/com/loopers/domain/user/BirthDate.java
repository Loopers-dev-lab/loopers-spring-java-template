package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

@Embeddable
public class BirthDate {
    private String birthDate;

    protected BirthDate() {
    }

    public BirthDate(String birthDate) {
        //생년월일 validation check
        if (birthDate == null || birthDate.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.");
        }
        if (!birthDate.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일이 `yyyy-MM-dd` 형식에 맞아야 합니다.");
        }
        this.birthDate = birthDate;
    }

    public String birthDate() {
        return birthDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BirthDate birthDate1 = (BirthDate) o;
        return birthDate != null ? birthDate.equals(birthDate1.birthDate) : birthDate1.birthDate == null;
    }

    @Override
    public int hashCode() {
        return birthDate != null ? birthDate.hashCode() : 0;
    }
}
