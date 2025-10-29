package com.loopers.interfaces.api;

import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.user.UserCreateV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
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

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserModelV1ApiE2ETest {

  private final TestRestTemplate testRestTemplate;
  private final UserJpaRepository userJpaRepository;
  private final DatabaseCleanUp databaseCleanUp;

  @Autowired
  public UserModelV1ApiE2ETest(
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

  @Nested
  class Get {
    @Test
    void 존재하지_않는_ID_실패() {
      //given
      String userId = "user1";

      //when
      String url = "/api/v1/users/" + userId;
      ParameterizedTypeReference<ApiResponse<UserCreateV1Dto.UserResponse>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<UserCreateV1Dto.UserResponse>> res = testRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void 내정보조회_성공() {
      //given
      UserModel userModel = UserModel.create("user1", "user1@test.XXX", "1999-01-01", "F");
      userJpaRepository.save(userModel);

      //when
      String url = "/api/v1/users/" + userModel.getUserId();
      ParameterizedTypeReference<ApiResponse<UserCreateV1Dto.UserResponse>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<UserCreateV1Dto.UserResponse>> res = testRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(res.getBody().data().userId()).isNotNull();
      assertThat(res.getBody().data().userId()).isEqualTo(userModel.getUserId());
    }
  }
}
