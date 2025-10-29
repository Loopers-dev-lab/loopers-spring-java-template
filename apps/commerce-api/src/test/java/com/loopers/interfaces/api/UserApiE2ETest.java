package com.loopers.interfaces.api;

import com.loopers.interfaces.api.user.UserDto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserApiE2ETest {

  private static final String ENDPOINT_REGISTER = "/api/v1/users/register";

  private final TestRestTemplate testRestTemplate;
  private final DatabaseCleanUp databaseCleanUp;

  @Autowired
  public UserApiE2ETest(
      TestRestTemplate testRestTemplate,
      DatabaseCleanUp databaseCleanUp
  ) {
    this.testRestTemplate = testRestTemplate;
    this.databaseCleanUp = databaseCleanUp;
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @DisplayName("POST /api/v1/users/register")
  @Nested
  class Register {

    @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
    @Test
    void returnsCreatedUserInfo_whenRegistrationIsSuccessful() {
      // given
      String userId = "testuser1";
      String email = "test@example.com";
      LocalDate birth = LocalDate.of(1990, 1, 1);
      String gender = "MALE";
      UserDto.RegisterRequest request = new UserDto.RegisterRequest(userId, email, birth, gender);

      // when
      ParameterizedTypeReference<ApiResponse<UserDto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<UserDto.UserResponse>> response =
          testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, new HttpEntity<>(request), responseType);

      // then
      assertAll(
          () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
          () -> assertThat(response.getBody().data().userId()).isEqualTo(userId),
          () -> assertThat(response.getBody().data().email()).isEqualTo(email),
          () -> assertThat(response.getBody().data().birth()).isEqualTo(birth),
          () -> assertThat(response.getBody().data().gender()).isEqualTo(gender)
      );
    }

    @DisplayName("회원 가입 시에 성별이 없을 경우, 400 Bad Request 응답을 반환한다.")
    @Test
    void throwsBadRequest_whenGenderIsMissing() {
      // given
      String userId = "testuser2";
      String email = "test2@example.com";
      LocalDate birth = LocalDate.of(1995, 5, 15);
      UserDto.RegisterRequest request = new UserDto.RegisterRequest(userId, email, birth, null);

      // when
      ParameterizedTypeReference<ApiResponse<UserDto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<UserDto.UserResponse>> response =
          testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, new HttpEntity<>(request), responseType);

      // then
      assertTrue(response.getStatusCode().is4xxClientError());
      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
  }
}
