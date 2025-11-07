package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@Entity
@Table(name = "users")
public class UserModel extends BaseEntity {

    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;
    
    private String email;
    private LocalDate birthDate;
    private String gender;
    private Integer point;

    protected UserModel() {}

    public UserModel(String id, String email, String birthDate, String gender) {
        validateId(id);
        validateEmail(email);
        validateBirthDate(birthDate);

        this.userId = id;
        this.email = email;
        this.birthDate = LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        this.gender = gender;
        this.point = 0;
    }

    private void validateId(String id) {
        if (id == null || id.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID는 비어있을 수 없습니다.");
        }
        if (id.length() > 10) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID는 10자 이내여야 합니다.");
        }
        if (!Pattern.matches("^[a-zA-Z0-9]+$", id)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID는 영문 및 숫자만 사용할 수 있습니다.");
        }
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일은 비어있을 수 없습니다.");
        }
        if (!Pattern.matches("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", email)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.");
        }
    }

    private void validateBirthDate(String birthDate) {
        if (birthDate == null || birthDate.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 비어있을 수 없습니다.");
        }
        try {
            LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeParseException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 yyyy-MM-dd 형식이어야 합니다.");
        }
    }

    public void chargePoint(Integer amount) {
        if (amount == null || amount <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전할 포인트는 0보다 큰 정수여야 합니다.");
        }
        this.point += amount;
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getGender() {
        return gender;
    }

    public Integer getPoint() {
        return point;
    }
}
