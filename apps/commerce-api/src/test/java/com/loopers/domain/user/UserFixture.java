package com.loopers.domain.user;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointFixture;
import org.instancio.Instancio;
import org.instancio.Model;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.instancio.Select.field;

public class UserFixture {

  private static final Model<User> USER_MODEL = Instancio.of(User.class)
      .ignore(field(User::getId))
      .ignore(field(User::getCreatedAt))
      .ignore(field(User::getUpdatedAt))
      .ignore(field(User::getDeletedAt))
      .set(field(User::getLoginId), generateLoginId())
      .generate(field(User::getEmail),
          gen -> gen.net().email()
      )
      .generate(field(User::getBirthday), gen -> gen.temporal()
          .localDate()
          .past()
      )
      .generate(field(User::getGender), gen ->
          gen.oneOf("F", "M")
      )
      .toModel();

  /**
   * User + Point 자동 연결
   */
  public static User createUser() {
    User user = Instancio.of(USER_MODEL).create();
    return user;
  }

  public static User createUser(Point point) {
    User user = Instancio.of(USER_MODEL).create();
    return user;
  }

  /**
   * 특정 필드만 override
   */
  public static User createUserWith(String loginId, String email, LocalDate birthday, String gender) {
    User user = Instancio.of(USER_MODEL)
        .set(field(User::getLoginId), loginId)
        .set(field(User::getEmail), email)
        .set(field(User::getBirthday), birthday)
        .set(field(User::getGender), gender)
        .create();

    Point point = PointFixture.createPoint(user);
    return user;
  }

  /**
   * loginId만 override
   */
  public static User createUserWithLoginId(String loginId) {
    User user = Instancio.of(USER_MODEL)
        .set(field(User::getLoginId), loginId)
        .create();
    Point point = PointFixture.createPoint(user);
    return user;
  }

  /**
   * 연관관계 없이 User만 생성하고 싶을 때
   */
  public static User createUserWithoutPoint() {
    return Instancio.of(USER_MODEL).create();
  }

  private static String generateLoginId() {
    Random random = new Random();
    String letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    String digits = "0123456789";

    // 최소 1글자씩 확보
    char letter = letters.charAt(random.nextInt(letters.length()));
    char digit = digits.charAt(random.nextInt(digits.length()));

    int remainingLength = random.nextInt(8) + 1; // 총 길이 2~10
    String allChars = letters + digits;
    StringBuilder sb = new StringBuilder();
    sb.append(letter).append(digit);

    for (int i = 2; i < remainingLength; i++) {
      sb.append(allChars.charAt(random.nextInt(allChars.length())));
    }

    List<Character> chars = sb.chars().mapToObj(c -> (char) c).collect(Collectors.toList());
    Collections.shuffle(chars);
    return chars.stream().map(String::valueOf).collect(Collectors.joining());
  }
}
