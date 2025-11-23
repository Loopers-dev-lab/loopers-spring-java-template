package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String loginId;
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private LocalDate birthDate;    // yyyy-MM-dd
    @Column(nullable = false)@Enumerated(EnumType.STRING)
    private Gender gender;

    private User(String loginId, String password, String email, LocalDate birthDate, Gender gender) {
        this.loginId = loginId;
        this.password = password;
        this.email = email;
        this.birthDate = birthDate;
        this.gender = gender;
    }

    public static User create(String loginId, String password, String email, String birthDate, Gender gender) {
        validLoginID(loginId);
        validEmail(email);
        LocalDate birthDateObj = validBirthDate(birthDate);
        validGender(gender);
        return new User(loginId, password, email, birthDateObj, gender);
    }

    private static LocalDate validBirthDate(String birthDate) {
        LocalDate birthDateObj;

        if (birthDate == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일이 입력되지 않았습니다.");
        }

        try {
            birthDateObj = LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 yyyy-MM-dd 형식이어야 합니다.");
        }

        if(birthDateObj.isAfter(LocalDate.now())) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 미래일 수 없습니다.");
        }

        return birthDateObj;
    }

    private static void validEmail(String email) {
        if (email == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일이 입력되지 않았습니다.");
        }
        if (!email.trim().matches("^[\\w.-]+@[\\w.-]+\\.[a-zA-Z]{2,}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.");
        }
    }

    private static void validLoginID(String loginId) {
        if (loginId == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID 가 입력되지 않았습니다.");
        }
        if (!loginId.trim().matches("^[a-zA-Z0-9]{1,10}$")) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID 형식이 올바르지 않습니다.");
        }
    }

    private static void validGender(Gender gender) {
        if (gender == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "성별 데이터가 입력되지 않았습니다.");
        }
    }
}
