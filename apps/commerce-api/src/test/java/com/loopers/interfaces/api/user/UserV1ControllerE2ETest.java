package com.loopers.interfaces.api.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
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
import org.springframework.http.*;

import java.math.BigDecimal;

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

    @DisplayName("POST /api/v1/users/new")
    @Nested
    class AccountUser {
        @DisplayName("회원 가입에 성공할 경우 유저 정보를 반환한다.")
        @Test
        void accountUserSuccess_returnUserInfo() {

            UserV1DTO.UserRequest request = new UserV1DTO.UserRequest(
                    "test123",
                    "test@test.com",
                 "1995-08-25",
                   Gender.MALE
                );

            HttpEntity<UserV1DTO.UserRequest> httpEntity = new HttpEntity<>(request);

            ParameterizedTypeReference<ApiResponse<UserV1DTO.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserV1DTO.UserResponse>> response =
                    testRestTemplate.exchange(
                                        "/api/v1/users/new",
                                        HttpMethod.POST,
                                        httpEntity,
                                        responseType
                                );

            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo("test123"),
                    () -> assertThat(response.getBody().data().email()).isEqualTo("test@test.com"),
                    () -> assertThat(response.getBody().data().birthdate()).isEqualTo("1995-08-25"),
                    () -> assertThat(response.getBody().data().gender()).isEqualTo(Gender.MALE)
            );

        }

        @DisplayName("회원 가입시 성별이 없는 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void whenGenderIsEmpty_returnBadRequest() {

            UserV1DTO.UserRequest request = new UserV1DTO.UserRequest(
                    "test123",
                    "test@test.com",
                    "1995-08-25",
                    null
            );

            HttpEntity<UserV1DTO.UserRequest> httpEntity = new HttpEntity<>(request);

            ParameterizedTypeReference<ApiResponse<UserV1DTO.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserV1DTO.UserResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/users/new",
                            HttpMethod.POST,
                            httpEntity,
                            responseType
                    );

            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }

    @DisplayName("GET /api/v1/users/{userId}")
    @Nested
    class GetUserInfo {
        @DisplayName("유저 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.")
        @Test
        void whenFindUserSuccess_returnUserInfo() {

            String userId = "test123";

            User user = User.builder()
                    .userId(userId)
                    .email("test@test.com")
                    .birthdate("1995-08-25")
                    .gender(Gender.MALE)
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
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo("test123"),
                    () -> assertThat(response.getBody().data().email()).isEqualTo("test@test.com"),
                    () -> assertThat(response.getBody().data().birthdate()).isEqualTo("1995-08-25"),
                    () -> assertThat(response.getBody().data().gender()).isEqualTo(Gender.MALE)
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

    @DisplayName("GET /api/v1/users/{userId}/point")
    @Nested
    class GetUserPoint {
        @DisplayName("유저 정보 조회에 성공할 경우, 해당하는 유저의 포인트 정보를 응답으로 반환한다.")
        @Test
        void whenFindUserSuccess_returnUserPoint() {

            String userId = "test123";

            User user = User.builder()
                    .userId(userId)
                    .email("test@test.com")
                    .birthdate("1995-08-25")
                    .gender(Gender.MALE)
                    .build();

            userJpaRepository.save(user);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", userId);

            HttpEntity<String> httpEntity = new HttpEntity<>(userId, headers);

            ParameterizedTypeReference<ApiResponse<UserV1DTO.UserPointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1DTO.UserPointResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/users/"+userId+"/point",
                            HttpMethod.GET,
                            httpEntity,
                            responseType
                    );

            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().point()).isNotNull()
            );

        }

        @DisplayName("Http 요청 헤더에 X-USER-ID 가 없는 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void whenHeaderToX_USER_IDIsNotExist_returnBadRequest () {
            String userId = "test123";

            User user = User.builder()
                    .userId(userId)
                    .email("test@test.com")
                    .birthdate("1995-08-25")
                    .gender(Gender.MALE)
                    .build();

            userJpaRepository.save(user);

            HttpEntity<String> httpEntity = new HttpEntity<>(userId, null);

            ParameterizedTypeReference<ApiResponse<UserV1DTO.UserPointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1DTO.UserPointResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/users/"+userId+"/point",
                            HttpMethod.GET,
                            httpEntity,
                            responseType
                    );

            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

    }

    @DisplayName("POST /api/v1/users/point/charge")
    @Nested
    class Point {

        @DisplayName("사용자 포인트 충전이 성공한 경우, 해당하는 사용자의 충전된 보유 포인트의 총량을 응답으로 반환한다.")
        @Test
        void whenChargePointSuccess_returnUserChargedPoint() {

            // 유저 생성
            String userId = "test123";
            BigDecimal chargePoint = BigDecimal.valueOf(1000);

            User user = User.builder()
                    .userId(userId)
                    .email("test@test.com")
                    .birthdate("1995-08-25")
                    .gender(Gender.MALE)
                    .build();

            User saved = userJpaRepository.save(user);

            // 유저의 기존 포인트
            BigDecimal originPoint = saved.getPoint();

            // 포인트 충전 request
            UserV1DTO.UserPointRequest request = new UserV1DTO.UserPointRequest(userId, chargePoint);

            HttpEntity<UserV1DTO.UserPointRequest> httpEntity = new HttpEntity<>(request);

            ParameterizedTypeReference<ApiResponse<UserV1DTO.UserPointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1DTO.UserPointResponse>> response =
                    testRestTemplate.exchange(
                            "/api/v1/users/point/charge",
                            HttpMethod.POST,
                            httpEntity,
                            responseType
                    );

            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().point()).isEqualByComparingTo(originPoint.add(chargePoint))
            );

        }

    }
}
