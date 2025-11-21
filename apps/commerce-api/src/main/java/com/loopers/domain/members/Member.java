package com.loopers.domain.members;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.loopers.domain.BaseEntity;
import com.loopers.domain.members.enums.Gender;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@NoArgsConstructor
@Getter
@Entity
@Table(name = "members")
public class Member extends BaseEntity {

    private static final Pattern MEMBER_ID_REGEX = Pattern.compile("^[a-zA-Z0-9]{1,10}$");
    private static final Pattern EMAIL_REGEX = Pattern.compile("^[^@]+@[^@]+\\.[^@]+$");
    private static final DateTimeFormatter BIRTH_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Column(name = "member_id", nullable = false, unique = true, length = 10)
    private String memberId;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "password", nullable = false)
    @JsonIgnore
    private String password;

    public Member(String memberId, String email, String password, String birthDate, Gender gender) {
        validateMemberId(memberId);
        validateEmail(email);
        validateGender(gender);

        this.memberId = memberId;
        this.email = email;
        this.password = password;
        this.birthDate = parseBirthDate(birthDate);
        this.gender = gender;
    }

    private static void validateMemberId(String memberId) {
        if (!MEMBER_ID_REGEX.matcher(memberId).matches()) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "ID는 영문 및 숫자 10자 이내여야 합니다."
            );
        }
    }

    private static void validateEmail(String email) {
        if (!EMAIL_REGEX.matcher(email).matches()) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "이메일은 xx@yy.zz 형식이어야 합니다."
            );
        }
    }

    public static void validateGender(Gender gender) {
        if (gender == null) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "성별은 필수입니다."
            );
        }
    }

    private static LocalDate parseBirthDate(String birthDate) {
        try {
            return LocalDate.parse(birthDate, BIRTH_DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            throw new CoreException(
                    ErrorType.BAD_REQUEST,
                    "생년월일은 yyyy-MM-dd 형식이어야 합니다."
            );
        }
    }

}
