package com.loopers.interfaces.api;

import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
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

  @Nested
  class Get {
    @Test
    void 포인트조회_성공() {
      //given
      BigDecimal JOIN_POINT = BigDecimal.TEN;

      UserModel userModel = UserModel.create("user1", "user1@test.XXX", "1999-01-01", "F");
      userJpaRepository.save(userModel);

      HttpHeaders headers = new HttpHeaders();
      headers.set("X-USER-ID", userModel.getUserId());

      //when
      String url = "/api/v1/user/point";
      ParameterizedTypeReference<ApiResponse<BigDecimal>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<BigDecimal>> res = testRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(res.getBody().data()).isNotNull();
      assertEquals(0, res.getBody().data().compareTo(JOIN_POINT));
    }

    @Test
    void ID없음_실패() {
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
}
