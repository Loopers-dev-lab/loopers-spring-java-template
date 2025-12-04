package com.loopers.domain.point;

import com.loopers.domain.user.User;
import com.loopers.domain.user.UserFixture;
import org.instancio.Instancio;
import org.instancio.Model;

import java.math.BigDecimal;

import static org.instancio.Select.field;

public class PointFixture {
  private static final Model<Point> POINT_MODEL = Instancio.of(Point.class)
      .ignore(field(Point::getId))
      .ignore(field(Point::getCreatedAt))
      .ignore(field(Point::getUpdatedAt))
      .ignore(field(Point::getDeletedAt))
      .ignore(field(Point::getUser))
      .generate(field(Point::getAmount), gen -> gen.math().bigDecimal().min(BigDecimal.ZERO)
      )
      .toModel();

  public static Point createPoint() {
    Point point = Instancio.of(POINT_MODEL).create();
    User user = UserFixture.createUser(point);
    point = point.create(user, point.getAmount());
    return point;
  }

  /**
   * 특정 필드만 override
   */
  public static Point createPointWith(BigDecimal amount) {
    Point point = Instancio.of(POINT_MODEL)
        .set(field(Point::getAmount), amount)
        .create();

    User user = UserFixture.createUser(point);
    point = point.create(user, point.getAmount());
    return point;
  }

  public static Point createPoint(User user) {
    return Point.create(user, BigDecimal.TEN);
  }
}
