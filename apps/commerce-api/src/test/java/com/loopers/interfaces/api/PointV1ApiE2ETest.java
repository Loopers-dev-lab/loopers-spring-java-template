package com.loopers.interfaces.api;

import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.user.UserJpaRepository;
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
  private final UserJpaRepository userJpaRepository;
  private final DatabaseCleanUp databaseCleanUp;

  @Autowired
  public PointV1ApiE2ETest(
      TestRestTemplate testRestTemplate,
      UserJpaRepository userJpaRepository,
      DatabaseCleanUp databaseCleanUp
  ) {
    this.testRestTemplate = testRestTemplate;
    this.userJpaRepository = userJpaRepository;
    this.databaseCleanUp = databaseCleanUp;
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("포인트 조회")
  @Nested
  class Get {
    @DisplayName("E2E테스트1-포인트 조회에할 경우, 보유 포인트가 반환")
    @Test
    void 성공_포인트조회() {
      //given
      BigDecimal JOIN_POINT = BigDecimal.TEN;

      UserModel userModel = UserModel.create("user1", "user1@test.XXX", "1999-01-01", "F");
      userJpaRepository.save(userModel);

      //when
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-USER-ID", userModel.getUserId());

      String url = "/api/v1/user/point";
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
      String url = "/api/v1/user/point";
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
      BigDecimal CHARGE_POINT = BigDecimal.TEN;

      UserModel userModel = UserModel.create("user1", "user1@test.XXX", "1999-01-01", "F");
      userJpaRepository.save(userModel);

      HttpHeaders headers = new HttpHeaders();
      headers.set("X-USER-ID", userModel.getUserId());

      //when
      String url = "/api/v1/user/point/charge";
      ParameterizedTypeReference<ApiResponse<BigDecimal>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<BigDecimal>> res = testRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(new BigDecimal(1_000), headers), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(res.getBody().data()).isNotNull();
      assertEquals(0, res.getBody().data().compareTo(new BigDecimal(1_010)));
    }

    @DisplayName("E2E테스트2-X-존재하지 않는 유저로 요청할 경우, 400 Not Found 응답을 반환")
    @Test
    void 실패_ID없음_400() {
      //given
      HttpHeaders headers = new HttpHeaders();
      headers.set("X-USER-ID", "user1");

      //when
      String url = "/api/v1/user/point/charge";
      ParameterizedTypeReference<ApiResponse<BigDecimal>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<BigDecimal>> res = testRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(BigDecimal.TEN, headers), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
  }
}
