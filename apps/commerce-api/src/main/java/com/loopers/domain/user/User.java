package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.point.Point;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.DynamicUpdate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

@Entity
@Table(name = "user")
@Getter
public class User extends BaseEntity {
  private String loginId;
  private String email;
  private LocalDate birthday;
  private String gender;

  protected User() {
  }

  private User(String loginId, String email, String birthday, String gender) {

    if (!Pattern.compile("^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9]{1,10}$").matcher(loginId).matches()) {
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
    this.loginId = loginId;
    this.email = email;
    this.birthday = LocalDate.parse(birthday);
    this.gender = gender;
  }

  public static User create(String userId, String email, String birthday, String gender) {
    return new User(userId, email, birthday, gender);
  }

}
