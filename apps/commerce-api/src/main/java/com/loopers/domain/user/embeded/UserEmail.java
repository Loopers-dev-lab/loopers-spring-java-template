package com.loopers.domain.user.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import lombok.Getter;

@Embeddable
@Getter
public class UserEmail {
    private String userEmail;

    private static final String EREGEX = "^\\S+@\\S+\\.\\S+$";

    public UserEmail() {
    }

    public UserEmail(String userEmail) {
        if(!userEmail.matches(EREGEX)) {
            throw new CoreException(ErrorType.BAD_REQUEST,"email은 올바른 형식으로 입력해야 합니다.");
        }
        this.userEmail = userEmail;
    }
}
