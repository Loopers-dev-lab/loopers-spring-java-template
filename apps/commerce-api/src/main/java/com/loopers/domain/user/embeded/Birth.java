package com.loopers.domain.user.embeded;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embeddable;
import lombok.Getter;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Embeddable
@Getter
public class Birth {
    private LocalDate birth;

    public Birth() {
    }

    public Birth(String birth) {
        try {
            LocalDate localDate = LocalDate.parse(birth, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            if(localDate.isAfter(LocalDate.now())){
                throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 현재 날짜 이전이어야 합니다.");
            }
            this.birth = localDate;
        }catch (DateTimeException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일 형식이 잘못되었습니다. 올바른 형식은 yyyy-MM-dd 입니다.");
        }
    }

}
