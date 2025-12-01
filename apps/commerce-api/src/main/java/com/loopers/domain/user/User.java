package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.regex.Pattern;

@Entity
@Table(name = "user")
@Getter
public class User extends BaseEntity {

    private static final Pattern USERID_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,10}$");
    private static final Pattern BIRTH_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[a-zA-Z]{2,}$");

    @Column(unique = true, nullable = false)
    private String userId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String birth;

    @Column(nullable = false)
    private String gender;

    protected User() {}

    public User(String userId, String email, String birth, String gender) {
        this.userId = requireValidUserId(userId);
        this.email = requireValidEmail(email);
        this.birth = requireValidBirthDate(birth);
        this.gender = requireValidGender(gender);
    }

    String requireValidUserId(String userId) {
        if(userId == null || userId.isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID가 비어있을 수 없습니다.");
        }

        if (!USERID_PATTERN.matcher(userId).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "사용자 ID가 영문 및 숫자 10자 이내여야 합니다.");
        }
        return userId;
    }

    String requireValidEmail(String email) {
        if(email == null || email.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일이 비어 있을 수 없습니다.");
        }

        if(!EMAIL_PATTERN.matcher(email).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일 형식에 맞지 않습니다. ex)xx@yy.zz");
        }
        return email;
    }

    String requireValidBirthDate(String birth) {
        if (birth == null || birth.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일이 비어 있을 수 없습니다.");
        }

        if(!BIRTH_PATTERN.matcher(birth).matches()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일 형식이 맞지 않습니다.");
        }
        return birth;
    }

    String requireValidGender(String gender) {
        if(gender == null || gender.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "성별이 비어있습니다.");
        }
        return gender;
    }
}
