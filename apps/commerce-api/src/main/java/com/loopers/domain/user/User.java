package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.like.Like;
import com.loopers.domain.point.Point;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class User extends BaseEntity {

    private String loginId;
    private String email;
    private String birthday;
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @OneToOne(mappedBy = "user")
    private Point point;

    @OneToMany(mappedBy = "user")
    private List<Like> likes;

    @Builder
    private User(
        String loginId
        , String email
        , String birthday
        , Gender gender
        , Point point
        , List<Like> likes
    ) {
        this.loginId = loginId;
        this.email = email;
        this.birthday = birthday;
        this.gender = gender;
        this.point = point;
        this.likes = likes;
        this.guard();
    }

    // 포인트 필드를 세팅 (초기화 위해 있음)
    public void setPoint(Point point) {
        if(this.point != null) {
            throw new CoreException(ErrorType.CONFLICT, "User : Point 가 이미 존재합니다.");
        }
        this.point = point;
    }

    // 유효성 검사
    @Override
    protected void guard() {
        // id : 영문, 숫자가 각각 최소 한 개씩 포함된 10자 이내 문자열 검사
        if(loginId == null || loginId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "User : ID가 비어있을 수 없습니다.");
        } else if(!loginId.matches("^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9]{1,10}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "User : ID가 비어있을 수 없습니다.ID에 영문과 숫자만 10자 이내로 꼭 포함되어 있어야 합니다.");
        }

        // email : `xx@yy.zz` 형식에 맞는지 검사
        if(email == null || email.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "User : email이 비어있을 수 없습니다.");
        } else if(!email.matches("^[a-zA-Z0-9]+@[a-zA-Z0-9]+\\.[a-zA-Z]{2,6}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "User : ID가 비어있을 수 없습니다.email이 `xx@yy.zz` 형식에 맞아야 합니다.");
        }

        // birthday : `yyyy-MM-dd` 형식에 맞는지 검사
        if(birthday == null || birthday.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "User : birthday가 비어있을 수 없습니다.");
        } else if(!birthday.matches("^\\d{4}-\\d{2}-\\d{2}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "User : ID가 비어있을 수 없습니다.birthday가 `yyyy-MM-dd` 형식에 맞아야 합니다.");
        }

        // gender : NULL 이면 안 됨
        if(gender == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "User : gender가 NULL일 수 없습니다.");
        }
        //throw new CoreException(ErrorType.BAD_REQUEST, "gender가 `MALE`, `FEMALE`, `OTHER` 형식에 맞아야 합니다.");
        //this.gender = Gender.valueOf(gender.toUpperCase());

        // point : NULL 이면 안 됨
//        if(point == null) {
//            throw new CoreException(ErrorType.BAD_REQUEST, "User : point가 비어있을 수 없습니다.");
//        }
    }

}
