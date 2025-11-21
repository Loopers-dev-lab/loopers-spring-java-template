package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.point.Point;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

  private static final String ID_PATTERN = "^[a-zA-Z0-9]{1,10}$";
  private static final String EMAIL_PATTERN = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$";
  private static final String BIRTHDATE_PATTERN = "^\\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\\d|3[01])$";

  private String userId;
  private String email;
  private String birthdate;
  private Gender gender;
  @Embedded
  private Point point;

  public enum Gender {
    MALE, FEMALE
  }

  public User(String userId, String email, String birthdate, Gender gender) {

    if (userId == null || !userId.matches(ID_PATTERN)) {
      throw new CoreException(ErrorType.BAD_REQUEST, "ID는 영문 및 숫자를 포함한 1~10자 형식에 맞아야 합니다.");
    }

    if (email == null || !email.matches(EMAIL_PATTERN)) {
      throw new CoreException(ErrorType.BAD_REQUEST, "이메일이 'xx@yy.zz' 형식에 맞지 않습니다.");
    }

    if (birthdate == null || !birthdate.matches(BIRTHDATE_PATTERN)) {
      throw new CoreException(ErrorType.BAD_REQUEST, "생년월일이 'yyyy-MM-dd' 형식에 맞지 않습니다.");
    }

    try {
      LocalDate.parse(birthdate);
    } catch (DateTimeParseException e) {
      throw new CoreException(ErrorType.BAD_REQUEST, "생년월일이 유효하지 않은 날짜입니다.");
    }

    if (gender == null) {
      throw new CoreException(ErrorType.BAD_REQUEST, "성별을 추가하셔야 합니다.");
    }

    this.userId = userId;
    this.email = email;
    this.birthdate = birthdate;
    this.gender = gender;
    this.point = new Point(0);
  }

  public User(String userId, String email, String birthdate, Gender gender, Point point) {
    this.userId = userId;
    this.email = email;
    this.birthdate = birthdate;
    this.gender = gender;
    this.point = point;
  }

  public String getUserId() {
    return userId;
  }

  public String getEmail() {
    return email;
  }

  public String getBirthdate() {
    return birthdate;
  }

  public Gender getGender() {
    return gender;
  }

  public Point getPoint() {
    return point;
  }

  public void usePoint(long amount) {
    this.point = this.point.subtract(amount);
  }
}
