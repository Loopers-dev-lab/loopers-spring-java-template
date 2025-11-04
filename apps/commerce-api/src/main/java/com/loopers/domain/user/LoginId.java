package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LoginId {

    @Column(nullable = false)
    private String id;

    public LoginId(String id) {
        validate(id);
        this.id = id;
    }

    private void validate(String loginId) {
        if (loginId == null || loginId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 필수입니다.");
        }
        if (!loginId.matches("^[a-zA-Z0-9]{1,10}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 영문과 숫자로만 구성되어야 하며, 10자 이내여야 합니다.");
        }
    }

}
