package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

@Entity
@Table(name = "User")
@Getter
public class User extends BaseEntity {
  private String userId;
  private String email;
  private LocalDate birthday;
  private String gender;

  protected User() {
  }

  private User(String userId, String email, String birthday, String gender) {
    if (email == null || !email.contains("@")) {
      throw new CoreException(
          ErrorType.BAD_REQUEST,
          "이메일 형식이 잘못되었습니다."
      );
    }
    try {
      this.birthday = LocalDate.parse(birthday);
    } catch (DateTimeParseException e) {
      throw new CoreException(
          ErrorType.BAD_REQUEST,
          "생년월일 형식이 유효하지 않습니다. (예: yyyy-MM-dd)"
      );
    }
    if (gender == null || gender.isBlank()) {
      throw new CoreException(
          ErrorType.BAD_REQUEST,
          "성별정보가 없습니다."
      );
    }
    this.userId = userId;
    this.email = email;
    this.birthday = LocalDate.parse(birthday);
    this.gender = gender;
  }

  public static User create(String userId, String email, String birthday, String gender) {
    return new User(userId, email, birthday, gender);
  }

}
