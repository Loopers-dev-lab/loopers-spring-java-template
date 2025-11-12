package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

@Embeddable
public record Email(String email) {

    public Email {
        //이메일 validation check
        if ( email == null || email.isBlank() ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.");
        }
        if ( !email.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$") ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일이 `xx@yy.zz` 형식에 맞아야 합니다.");
        }
    }

}
