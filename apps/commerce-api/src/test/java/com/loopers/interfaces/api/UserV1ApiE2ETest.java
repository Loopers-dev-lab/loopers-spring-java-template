package com.loopers.interfaces.api;

import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserService;
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

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserV1ApiE2ETest {

  private static final String ENDPOINT = "/api/v1/user";
  private static final Function<String, String> ENDPOINT_GET = id -> "/api/v1/user/" + id;

  private final TestRestTemplate testRestTemplate;
  private final UserService userService;
  private final DatabaseCleanUp databaseCleanUp;

  @Autowired
  public UserV1ApiE2ETest(
      TestRestTemplate testRestTemplate,
      UserService userService,
      DatabaseCleanUp databaseCleanUp
  ) {
    this.testRestTemplate = testRestTemplate;
    this.userService = userService;
    this.databaseCleanUp = databaseCleanUp;
  }

  @AfterEach
  void tearDown() {
    databaseCleanUp.truncateAllTables();
  }

  @Nested
  class Join {
    @Test
    void 성공_회원가입() {
      //given
      UserCreateV1Dto.UserRequest req = new UserCreateV1Dto.UserRequest("user1", "user1@test.XXX", "1999-01-01", "F");

      //when
      ParameterizedTypeReference<ApiResponse<UserCreateV1Dto.UserResponse>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<UserCreateV1Dto.UserResponse>> res = testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, new HttpEntity<>(req), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(res.getBody().data().userId()).isNotNull();
      assertThat(res.getBody().data().userId()).isEqualTo(req.userId());
    }

    @Test
    void 실패_회원가입_성별없음() {
      //given
      UserCreateV1Dto.UserRequest req = new UserCreateV1Dto.UserRequest("user1", "user1@test.XXX", "1999-01-01", null);

      //when
      ParameterizedTypeReference<ApiResponse<UserCreateV1Dto.UserResponse>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<UserCreateV1Dto.UserResponse>> res = testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, new HttpEntity<>(req), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
      assertThat(res.getBody().meta().message()).contains("성별정보가 없습니다.");
    }
  }

  @Nested
  class Get {
    @Test
    void 실패_존재하지_않는_ID() {
      //given
      String userId = "user1";

      //when
      String url = ENDPOINT_GET.apply(userId);
      ParameterizedTypeReference<ApiResponse<UserCreateV1Dto.UserResponse>> resType = new ParameterizedTypeReference<>() {
      };
      ResponseEntity<ApiResponse<UserCreateV1Dto.UserResponse>> res = testRestTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(null), resType);

      //then
      assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void 성공_정보조회() {
      //given
      UserModel userModel = UserModel.create("user1", "user1@test.XXX", "1999-01-01", "F");
      userService.join(userModel);

      //when
      String url = ENDPOINT_GET.apply(userModel.getUserId());
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
