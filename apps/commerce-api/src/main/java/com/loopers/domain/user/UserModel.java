package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

@Entity
@Table(name = "users")
public class UserModel extends BaseEntity {
    @Column(name = "user_id", unique = true, nullable = false)
    private String userId;
    @Column(name = "user_name", nullable = false)
    private String userName;
    private String description;
    private String email;
    private String birthDate;
    private String gender;
    private Integer point;

    protected UserModel() {}
    
    public UserModel(String userId, String name, String description, String email
            , String birthDate, String gender, Integer point) {
        validateUserId(userId);
        validateName(name);
        validateEmail(email);
        validateBirthDate(birthDate);
        validateGender(gender);
        validateInitPoint(point);

        this.userId = userId;
        this.userName = name;
        this.description = description;
        this.email = email;
        this.birthDate = birthDate;
        this.gender = gender;
        this.point = point;
    }

    public UserModel(String userId, String name, String description, String email, String birthDate, String gender) {
        this(userId, name, description, email, birthDate, gender, 0);
    }

    public void updateDescription(String newDescription) {
        if (newDescription == null || newDescription.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "설명은 비어있을 수 없습니다.");
        }
        this.description = newDescription;
    }

    public void updatePoint(Integer newPoint) {
        validateChargePoint(newPoint);
        this.point +=  newPoint;
    }

    public void decreasePoint(Integer decreasePoint) {
        hasEnoughPoint(decreasePoint);
        this.point -= decreasePoint;
    }

    public boolean hasEnoughPoint(Integer decreasePoint) {
        validatePositivePoint(decreasePoint);
        return this.point >= decreasePoint;
    }


    private void validateNotBlank(String field, String message) {
        if (field == null || field.isBlank()) {
            throw new CoreException(ErrorType.BAD_REQUEST, message);
        }
    }

    private void validateUserId(String id) {
        validateNotBlank(id, "ID는 비어있을 수 없습니다.");
        if (id.length() > 10) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID 길이는 10자리를 넘을 수 없습니다.");
        }

        String validUserIdRegex = "^[a-zA-Z0-9]+$";
        if (!Pattern.matches(validUserIdRegex, id)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "ID는 영문 및 숫자만 사용할 수 있습니다.");
        }
    }

    private void validateName(String name) {
        validateNotBlank(name, "이름은 비어있을 수 없습니다.");
    }

    private void validateEmail(String email) {
        validateNotBlank(email, "이메일은 비어있을 수 없습니다.");

        String validEmailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        if (!Pattern.matches(validEmailRegex, email)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "이메일 입력형식이 잘못되었습니다.");

        }
    }

    private void validateBirthDate(String birthDate) {
        validateNotBlank(birthDate, "생년월일을 입력해주세요.");
        try {
            LocalDate.parse(birthDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (DateTimeException e) {
            throw new CoreException(ErrorType.BAD_REQUEST, "생년월일은 yyyy-MM-dd 형식이어야 합니다.");
        }
    }

    private void validateInitPoint(Integer point) {
        if (point == null || point < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "신규 포인트는 음수일 수 없습니다.");
        }
    }

    private void validateChargePoint(Integer point) {
        if (point == null || point <= 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "충전 포인트는 0 이하일 수 없습니다.");
        }
    }

    private void validatePositivePoint(Integer point) {
        if (point == null || point < 0) {
            throw new CoreException(ErrorType.BAD_REQUEST, "포인트는 양수로 입력되어야 합니다.");
        }
    }

    private void validateGender(String gender) {
        validateNotBlank(gender, "성별은 비어있을 수 없습니다.");
    }

    public String getUserId() {
        return userId;
    }

    public String getUserName() {
        return userName;
    }

    public String getDescription() {
        return description;
    }

    public String getEmail() {
        return email;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public String getGender() {
        return gender;
    }

    public Integer getPoint() {
        return point;
    }
}
