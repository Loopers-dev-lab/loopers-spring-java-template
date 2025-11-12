package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.Table;
import java.util.regex.Pattern;
import lombok.Getter;

import java.time.Clock;
import java.time.LocalDate;

@Entity
@Table(name = "users")
@Getter
public class User extends BaseEntity {

  private static final int MAX_LOGIN_ID_LENGTH = 10;
  // 영문 대소문자 + 숫자만 허용 (특수문자, 공백, 한글 불가)
  private static final Pattern LOGIN_ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
  // 이메일 형식: xxx@yyy.zzz (공백 불가)
  private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  @Column(name = "login_id", unique = true, nullable = false, length = MAX_LOGIN_ID_LENGTH)
  private String loginId;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private LocalDate birth; //yyyy-MM-dd

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private Gender gender;

  protected User() {
  }

  private User(String loginId, String email, LocalDate birth, Gender gender, Clock clock) {
    String normalizedLoginId = loginId != null ? loginId.trim() : null;
    String normalizedEmail = email != null ? email.toLowerCase().trim() : null;

    validateLoginId(normalizedLoginId);
    validateEmail(normalizedEmail);
    validateBirth(birth, LocalDate.now(clock));
    validateGender(gender);

    this.loginId = normalizedLoginId;
    this.email = normalizedEmail;
    this.birth = birth;
    this.gender = gender;
  }

  public static User of(String loginId, String email, LocalDate birth, Gender gender) {
    return of(loginId, email, birth, gender, Clock.systemDefaultZone());
  }

  public static User of(String loginId, String email, LocalDate birth, Gender gender, Clock clock) {
    return new User(loginId, email, birth, gender, clock);
  }

  private void validateLoginId(String loginId) {
    if (loginId == null || loginId.isBlank()) {
      throw new CoreException(ErrorType.INVALID_LOGIN_ID_EMPTY);
    }
    if (loginId.length() > MAX_LOGIN_ID_LENGTH) {
      throw new CoreException(ErrorType.INVALID_LOGIN_ID_LENGTH);
    }
    if (!LOGIN_ID_PATTERN.matcher(loginId).matches()) {
      throw new CoreException(ErrorType.INVALID_LOGIN_ID_FORMAT);
    }
  }

  private void validateEmail(String email) {
    if (email == null || email.isBlank()) {
      throw new CoreException(ErrorType.INVALID_EMAIL_EMPTY);
    }
    if (!EMAIL_PATTERN.matcher(email).matches()) {
      throw new CoreException(ErrorType.INVALID_EMAIL_FORMAT);
    }
  }

  private void validateBirth(LocalDate birth, LocalDate currentDate) {
    if (birth == null) {
      throw new CoreException(ErrorType.INVALID_BIRTH_EMPTY);
    }
    if (birth.isAfter(currentDate)) {
      throw new CoreException(ErrorType.INVALID_BIRTH_FUTURE);
    }
  }

  private void validateGender(Gender gender) {
    if (gender == null) {
      throw new CoreException(ErrorType.INVALID_GENDER_EMPTY);
    }
  }
}
