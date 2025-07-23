package com.loopers.domain.user.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class UserLoginId {
    private String loginId;

    private static final String REGEX = "^[a-zA-Z0-9]{1,10}$";
    public UserLoginId() {
    }

    public UserLoginId(String loginId) {
        if(loginId.trim().isBlank() || loginId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST,"loginId는 빈칸일수 없습니다.");
        }
        if(!loginId.matches(REGEX)) {
            throw new CoreException(ErrorType.BAD_REQUEST,"loginId는 영문 및 숫자 10자 이내로 입력해야 합니다.");
        }
        this.loginId = loginId;
    }

}
