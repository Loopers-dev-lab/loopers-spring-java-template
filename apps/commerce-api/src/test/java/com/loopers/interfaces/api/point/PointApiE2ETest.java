package com.loopers.interfaces.api.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.interfaces.api.point.PointDto.ChargeRequest;
import com.loopers.interfaces.api.point.PointDto.ChargeResponse;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PointApiE2ETest {

  private static final Clock TEST_CLOCK = Clock.fixed(
      Instant.parse("2025-10-30T00:00:00Z"),
      ZoneId.systemDefault()
  );
  private static final String ENDPOINT_GET = "/api/v1/points";
  private static final String ENDPOINT_CHARGE = "/api/v1/points/charge";
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

  @DisplayName("GET /api/v1/points (header: X-USER-LOGIN-ID) ")
  @Nested
  class GetPoint {

    @Test
    @DisplayName("`X-USER-LOGIN-ID` 헤더가 없을 경우, `400 Bad Request` 응답을 반환한다.")
    void returnBadRequestException_whenUserIdHeaderIsMissing() {
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

      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
      );

    }

    @Test
    @DisplayName("포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.")
    void returnsPoint_whenPointExists() {
      //given
      String userId = "testuser";
      String email = "test@example.com";
      LocalDate birth = LocalDate.of(1990, 1, 1);
      Gender gender = Gender.MALE;

      User user = User.of(userId, email, birth, gender, TEST_CLOCK);
      User savedUser = userJpaRepository.save(user);

      Point point = Point.of(savedUser.getId(), 5L);
      pointJpaRepository.save(point);

      // when
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.set("X-USER-LOGIN-ID", userId);

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
          () -> assertThat(response.getBody()).isNotNull(),
          () -> assertThat(response.getBody().data().userId()).isEqualTo(savedUser.getId()),
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
      httpHeaders.set("X-USER-LOGIN-ID", userId);

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
          () -> assertThat(response.getBody()).isNotNull(),
          () -> assertThat(response.getBody().data()).isNull()
      );
    }

  }

  @DisplayName("PATCH /api/v1/points/charge (header: X-USER-LOGIN-ID, body: ChargeRequest)")
  @Nested
  class ChargePoint {

    @Test
    @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다")
    void returnsTotalBalance_whenChargingThousand() {
      // given
      String userId = "testuser";
      String email = "test@example.com";
      LocalDate birth = LocalDate.of(1990, 1, 1);
      Gender gender = Gender.MALE;

      User user = User.of(userId, email, birth, gender, TEST_CLOCK);
      User savedUser = userJpaRepository.save(user);

      Point point = Point.zero(savedUser.getId());
      pointJpaRepository.save(point);

      ChargeRequest chargeRequest = new ChargeRequest(1000L);

      // when
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.set("X-USER-LOGIN-ID", userId);

      ParameterizedTypeReference<ApiResponse<ChargeResponse>> responseType =
          new ParameterizedTypeReference<>() {
          };
      ResponseEntity<ApiResponse<ChargeResponse>> response =
          testRestTemplate.exchange(
              ENDPOINT_CHARGE,
              HttpMethod.PATCH,
              new HttpEntity<>(chargeRequest, httpHeaders),
              responseType
          );

      // then
      assertAll(
          () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
          () -> assertThat(response.getBody()).isNotNull(),
          () -> assertThat(response.getBody().data().userId()).isEqualTo(savedUser.getId()),
          () -> assertThat(response.getBody().data().balance()).isEqualTo(1000L)
      );
    }

    @Test
    @DisplayName("존재하지 않는 유저로 요청할 경우, 404 NOT FOUND 응답을 반환한다.")
    void returnsNotFoundException_whenUserDoesNotExists() {
      // given
      String userId = "doesnotexist";
      ChargeRequest chargeRequest = new ChargeRequest(1000L);

      // when
      HttpHeaders httpHeaders = new HttpHeaders();
      httpHeaders.set("X-USER-LOGIN-ID", userId);

      ParameterizedTypeReference<ApiResponse<ChargeResponse>> responseType =
          new ParameterizedTypeReference<>() {
          };
      ResponseEntity<ApiResponse<ChargeResponse>> response =
          testRestTemplate.exchange(
              ENDPOINT_CHARGE,
              HttpMethod.PATCH,
              new HttpEntity<>(chargeRequest, httpHeaders),
              responseType
          );

      // then
      assertAll(
          () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
      );
    }
  }
}
