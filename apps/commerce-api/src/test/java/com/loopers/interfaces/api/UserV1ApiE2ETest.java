package com.loopers.interfaces.api;

import com.loopers.infrastructure.example.ExampleJpaRepository;
import com.loopers.interfaces.api.user.UserCreateV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.assertj.core.api.Assertions;
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

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserV1ApiE2ETest {

  private final TestRestTemplate testRestTemplate;
  private final ExampleJpaRepository exampleJpaRepository;
  private final DatabaseCleanUp databaseCleanUp;

  @Autowired
  public UserV1ApiE2ETest(
      TestRestTemplate testRestTemplate,
      ExampleJpaRepository exampleJpaRepository,
      DatabaseCleanUp databaseCleanUp
  ) {
    this.testRestTemplate = testRestTemplate;
    this.exampleJpaRepository = exampleJpaRepository;
    this.databaseCleanUp = databaseCleanUp;
  }

  @Nested
  class Join {
    @Test
    void 회원가입_성공() {
      //given
      UserCreateV1Dto.UserRequest req = new UserCreateV1Dto.UserRequest("user1", "user1@test.XXX", "1999-01-01", "F");

      //when
      String url = "/api/v1/users";
      ParameterizedTypeReference<ApiResponse<UserCreateV1Dto.UserResponse>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<UserCreateV1Dto.UserResponse>> res = testRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(req), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(res.getBody().data().userId()).isNotNull();
      assertThat(res.getBody().data().userId()).isEqualTo(req.userId());
    }

    @Test
    void 회원가입_성별없음_실패() {
      //given
      UserCreateV1Dto.UserRequest req = new UserCreateV1Dto.UserRequest("user1", "user1@test.XXX", "1999-01-01", null);

      //when
      String url = "/api/v1/users";
      ParameterizedTypeReference<ApiResponse<UserCreateV1Dto.UserResponse>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<UserCreateV1Dto.UserResponse>> res = testRestTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(req), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(res.getBody().meta().message()).contains("성별정보가 없습니다.");
    }
  }
}
