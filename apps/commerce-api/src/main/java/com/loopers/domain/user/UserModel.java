package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@Entity
@Table(name = "user")
@Getter
public class UserModel extends BaseEntity {
  private String userId;
  private String email;
  private LocalDate birthday;
  private String gender;

  protected UserModel() {
  }

  private UserModel(String userId, String email, String birthday, String gender) {

    if (!Pattern.compile("^[a-zA-Z0-9]{1,10}$").matcher(userId).matches()) {
      throw new CoreException(
          ErrorType.BAD_REQUEST,
          "아이디 형식이 잘못되었습니다.(영문 및 숫자 1~10자 이내)"
      );
    }
    if (email == null || !email.contains("@")
        || !Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$").matcher(email).matches()) {
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
          "생년월일 형식이 유효하지 않습니다."
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

  public static UserModel create(String userId, String email, String birthday, String gender) {
    return new UserModel(userId, email, birthday, gender);
  }

}
