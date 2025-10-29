package com.loopers.interfaces.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.interfaces.api.point.PointDto.PointResponse;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.ApiResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PointApiE2ETest {

  private static final String ENDPOINT_GET = "/api/v1/points";
  private final TestRestTemplate testRestTemplate;
  private final UserJpaRepository userJpaRepository;
  private final PointJpaRepository pointJpaRepository;
  private final DatabaseCleanUp databaseCleanUp;

  @Autowired
  public PointApiE2ETest(
      TestRestTemplate testRestTemplate,
      UserJpaRepository userJpaRepository,
      PointJpaRepository pointJpaRepository,
      DatabaseCleanUp databaseCleanUp
  ) {
    this.testRestTemplate = testRestTemplate;
    this.userJpaRepository = userJpaRepository;
    this.pointJpaRepository = pointJpaRepository;
    this.databaseCleanUp = databaseCleanUp;
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("GET /api/v1/points (header: X-USER-ID) ")
  @Nested
  class GetPoint {

    @Test
    @DisplayName("`X-USER-ID` 헤더가 없을 경우, `400 Bad Request` 응답을 반환한다.")
    void returnBadRequestException_whenPointDoesNotExists() {
      ParameterizedTypeReference<ApiResponse<PointResponse>> responseType =
          new ParameterizedTypeReference<>() {
          };
      ResponseEntity<ApiResponse<PointResponse>> response =
          testRestTemplate.exchange(
              ENDPOINT_GET,
              HttpMethod.GET,
              new HttpEntity<>(null, null),
              responseType
          );

      assertTrue(response.getStatusCode().is4xxClientError());
    }

    @Test
    @DisplayName("포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.")
    void returnsPoint_whenPointExists() {
      //given
      String userId = "testuser";
      String email = "test@example.com";
      LocalDate birth = LocalDate.of(1990, 1, 1);
      Gender gender = Gender.MALE;

      User user = User.of(userId, email, birth, gender);
      User savedUser = userJpaRepository.save(user);

      Point point = Point.of(savedUser, 5L);
      pointJpaRepository.save(point);

      // when
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.set("X-USER-ID", userId);

      ParameterizedTypeReference<ApiResponse<PointResponse>> responseType =
          new ParameterizedTypeReference<>() {
          };
      ResponseEntity<ApiResponse<PointResponse>> response =
          testRestTemplate.exchange(
              ENDPOINT_GET,
              HttpMethod.GET,
              new HttpEntity<>(null, httpHeaders),
              responseType
          );

      //then
      assertAll(
          () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
          () -> assertThat(response.getBody().data().userId()).isEqualTo(userId),
          () -> assertThat(response.getBody().data().balance()).isEqualTo(5L)
      );
    }


    @Test
    @DisplayName("해당 ID의 회원이 존재하지 않을 경우, null을 반환한다.")
    void returnsNull_whenUserDoesNotExists() {
      //given
      String userId = "nonexistent";
      // when
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.set("X-USER-ID", userId);

      ParameterizedTypeReference<ApiResponse<PointResponse>> responseType =
          new ParameterizedTypeReference<>() {
          };
      ResponseEntity<ApiResponse<PointResponse>> response =
          testRestTemplate.exchange(
              ENDPOINT_GET,
              HttpMethod.GET,
              new HttpEntity<>(null, httpHeaders),
              responseType
          );

      //then
      assertAll(
          () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
          () -> assertThat(response.getBody().data()).isNull()
      );
    }

  }
}
