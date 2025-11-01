package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserEntity extends BaseEntity {
    private String loginId;
    private String email;
    private Gender gender;
    private String birth;
    private String password;

    public UserEntity(String loginId, String email, Gender gender, String birth, String password) {
        UserValidator.validateLoginId(loginId);
        UserValidator.validateEmail(email);
        UserValidator.validateBirth(birth);
        UserValidator.validatePassword(password);

        this.loginId = loginId;
        this.email = email;
        this.gender = gender;
        this.birth = birth;
        this.password = password;
    }

}
