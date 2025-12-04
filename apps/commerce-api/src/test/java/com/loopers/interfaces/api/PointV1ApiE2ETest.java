package com.loopers.interfaces.api;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointService;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserFixture;
import com.loopers.domain.user.UserService;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PointV1ApiE2ETest {

  private final TestRestTemplate testRestTemplate;
  private final UserService userService;
  private final PointService pointService;
  private final DatabaseCleanUp databaseCleanUp;

  @Autowired
  public PointV1ApiE2ETest(
      TestRestTemplate testRestTemplate,
      UserService userService,
      PointService pointService,
      DatabaseCleanUp databaseCleanUp
  ) {
    this.testRestTemplate = testRestTemplate;
    this.userService = userService;
    this.pointService = pointService;
    this.databaseCleanUp = databaseCleanUp;
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("포인트 조회")
  @Nested
  class Get {
    @DisplayName("E2E테스트1-포인트 조회할 경우, 보유 포인트가 반환")
    @Test
    void 성공_포인트조회() {
      //given
      BigDecimal JOIN_POINT = BigDecimal.TEN;

      User savedUser = userService.join(UserFixture.createUser());
      pointService.save(Point.create(savedUser, BigDecimal.TEN));
      //when
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-USER-ID", savedUser.getId().toString());

      String url = "/api/v1/users/point";
      ParameterizedTypeReference<ApiResponse<BigDecimal>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<BigDecimal>> res = testRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(res.getBody().data()).isNotNull();
      assertEquals(0, res.getBody().data().compareTo(JOIN_POINT));
    }

    @DisplayName("E2E테스트2-X-USER-ID가 없을 경우, 400 Bad Request 반환")
    @Test
    void 실패_ID없음_400() {
      //given

      //when
      String url = "/api/v1/users/point";
      ParameterizedTypeReference<ApiResponse<BigDecimal>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<BigDecimal>> res = testRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
  }

  @DisplayName("포인트 충전")
  @Nested
  class Charge {
    @DisplayName("E2E테스트1-존재하는 유저가 1000원을 충전할 경우, 충전된 보유총량을 응답으로 반환한다.")
    @Test
    void 성공_포인트충전() {
      //given
      BigDecimal chargeAmt = new BigDecimal(1_000);
      User savedUser = userService.join(UserFixture.createUser());
      pointService.save(Point.create(savedUser, BigDecimal.TEN));
      BigDecimal initialAmt = pointService.getAmount(savedUser.getId());

      HttpHeaders headers = new HttpHeaders();
      headers.set("X-USER-ID", savedUser.getId().toString());

      //when
      String url = "/api/v1/users/point/charge";
      ParameterizedTypeReference<ApiResponse<BigDecimal>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<BigDecimal>> res = testRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(chargeAmt, headers), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(res.getBody().data()).isNotNull();
      assertEquals(0, res.getBody().data().compareTo(initialAmt.add(chargeAmt)));
    }

    @DisplayName("E2E테스트2-존재하지 않는 유저로 요청할 경우, 404 Not Found 응답을 반환")
    @Test
    void 실패_ID없음_400() {
      //given
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-USER-ID", "999999");

      //when
      String url = "/api/v1/users/point/charge";
      ParameterizedTypeReference<ApiResponse<BigDecimal>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<BigDecimal>> res = testRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(BigDecimal.TEN, headers), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
  }
}
