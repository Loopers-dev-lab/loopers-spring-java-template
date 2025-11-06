package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.Objects;
import lombok.Getter;

@Getter
@Table(name = "example")
@Entity
public class UserModel extends BaseEntity {
    private String userId;
    private String email;
    private LocalDate birthday;
    private Gender gender;

    public static UserModel create(final String userId, final String email, final LocalDate birthday, final Gender gender) {
        validateUser(userId, email, gender);

        UserModel userModel = new UserModel();
        userModel.userId = userId;
        userModel.email = email;
        userModel.birthday = birthday;
        userModel.gender = gender;
        return userModel;
    }

    private static void validateUser(final String userId, final String email, final Gender gender) {
        if (Objects.isNull(userId) || userId.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID는 필수입니다.");
        }
        if (!userId.matches("^[a-zA-Z0-9]{1,10}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID는 영문 및 숫자로 이루어진 10글자 이내의 문자여야 합니다.");
        }
        if (!email.matches("^[\\w.%+-]+@[\\w.-]+\\.[a-zA-Z]{2,6}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일 형식에 맞지 않습니다.");
        }
        if (Objects.isNull(gender)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "성별은 필수 입니다.");
        }
    }
}
