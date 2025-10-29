package com.loopers.interfaces.api.user;

import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.ApiResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ControllerE2ETest {
    private final TestRestTemplate testRestTemplate;
    private final UserJpaRepository userJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public UserV1ControllerE2ETest(
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

    @DisplayName("POST /api/v1/users")
    @Nested
    class AccountUser {
        @DisplayName("회원 가입에 성공할 경우 유저 정보를 반환한다.")
        @Test
        void accountUserSuccess_returnUserInfo() {
            // given
            UserV1DTO.UserRequest request = new UserV1DTO.UserRequest(
                    "test123",
                    "test@test.com",
                 "1995-08-25",
                   "M"
                );

            HttpEntity<UserV1DTO.UserRequest> httpEntity = new HttpEntity<>(request);

            // when
            ParameterizedTypeReference<ApiResponse<UserV1DTO.UserResponse>> responseType = new ParameterizedTypeReference<ApiResponse<UserV1DTO.UserResponse>>() {};
            ResponseEntity<ApiResponse<UserV1DTO.UserResponse>> response =
                    testRestTemplate.exchange(
                                        "/api/v1/users/new",
                                        HttpMethod.POST,
                                        httpEntity,
                                        responseType
                                );

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo("test123"),
                    () -> assertThat(response.getBody().data().email()).isEqualTo("test@test.com"),
                    () -> assertThat(response.getBody().data().birthdate()).isEqualTo("1995-08-25"),
                    () -> assertThat(response.getBody().data().gender()).isEqualTo("M")
            );

        }

        @DisplayName("회원 가입시 성별이 없는 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void whenGenderIsEmpty_returnBadRequest() {

            // given
            UserV1DTO.UserRequest request = new UserV1DTO.UserRequest(
                    "test123",
                    "test@test.com",
                    "1995-08-25",
                    null
            );

            HttpEntity<UserV1DTO.UserRequest> httpEntity = new HttpEntity<>(request);

            // when
            ParameterizedTypeReference<ApiResponse<UserV1DTO.UserResponse>> responseType = new ParameterizedTypeReference<ApiResponse<UserV1DTO.UserResponse>>() {};
            ResponseEntity<ApiResponse<UserV1DTO.UserResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/users/new",
                            HttpMethod.POST,
                            httpEntity,
                            responseType
                    );

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }

    @DisplayName("GET /api/v1/users")
    @Nested
    class GetUserInfo {
        @DisplayName("유저 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.")
        @Test
        void whenFindUserSuccess_returnUserInfo() {

            String userId = "test123";

            UserModel user = UserModel.builder()
                    .userId("test123")
                    .email("test@test.com")
                    .birthdate("1995-08-25")
                    .gender("M")
                    .build();

            userJpaRepository.save(user);


            HttpEntity<String> httpEntity = new HttpEntity<>(userId);

            ParameterizedTypeReference<ApiResponse<UserV1DTO.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1DTO.UserResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/users/"+userId,
                            HttpMethod.GET,
                            httpEntity,
                            responseType
                    );

            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo("test123"),
                    () -> assertThat(response.getBody().data().email()).isEqualTo("test@test.com"),
                    () -> assertThat(response.getBody().data().birthdate()).isEqualTo("1995-08-25"),
                    () -> assertThat(response.getBody().data().gender()).isEqualTo("M")
            );

        }

        @DisplayName("존재하지 않는 ID 로 조회할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void whenFindUserIsEmpty_returnNull() {

            String userId = "test123";

            HttpEntity<String> httpEntity = new HttpEntity<>(userId);

            ParameterizedTypeReference<ApiResponse<UserV1DTO.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1DTO.UserResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/users/"+userId,
                            HttpMethod.GET,
                            httpEntity,
                            responseType
                    );

            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );

        }
    }
}
