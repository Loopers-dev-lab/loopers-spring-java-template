package com.loopers.domain.user;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;

@Embeddable
public record UserId(String userId) {

    public UserId{
        //유저아이디 체크
        if ( userId == null || userId.isBlank() ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "UserId는 비어있을 수 없습니다.");
        }
        if ( !userId.matches("^[a-zA-Z0-9_-]{1,10}$") ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID 가 `영문 및 숫자 10자 이내` 형식에 맞아야 합니다.");
        }
    }
}
