package com.loopers.interfaces.api;

import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.user.UserV1Dto;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest {

    private static final Function<String, String> ENDPOINT_GET_USER = id -> "/api/v1/users/" + id;
    private static final Function<String, String> ENDPOINT_GET_POINT = id -> "/api/v1/users/" + id + "/points";
    private static final Function<String, String> ENDPOINT_CHARGE_POINT = id -> "/api/v1/users/" + id + "/points/charge";

    private final TestRestTemplate testRestTemplate;
    private final UserJpaRepository userJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public UserV1ApiE2ETest(
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
    class CreateUser {
        
        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        @Test
        void returnsCreatedUserInfo_whenUserRegistrationSucceeds() {
            // arrange
            UserV1Dto.UserCreateRequest request = new UserV1Dto.UserCreateRequest(
                "user123", "test@example.com", "1990-01-01", "M"
            );
            HttpEntity<UserV1Dto.UserCreateRequest> httpEntity = new HttpEntity<>(request);

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                testRestTemplate.exchange("/api/v1/users", HttpMethod.POST, httpEntity, responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().id()).isEqualTo("user123"),
                () -> assertThat(response.getBody().data().email()).isEqualTo("test@example.com"),
                () -> assertThat(response.getBody().data().birthDate().toString()).isEqualTo("1990-01-01"),
                () -> assertThat(response.getBody().data().gender()).isEqualTo("M"),
                () -> assertThat(response.getBody().data().point()).isEqualTo(0)
            );
        }

        @DisplayName("회원 가입 시에 성별이 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenGenderIsMissing() {
            // arrange
            UserV1Dto.UserCreateRequest request = new UserV1Dto.UserCreateRequest(
                "user123", "test@example.com", "1990-01-01", null
            );
            HttpEntity<UserV1Dto.UserCreateRequest> httpEntity = new HttpEntity<>(request);

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                testRestTemplate.exchange("/api/v1/users", HttpMethod.POST, httpEntity, responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }

    @DisplayName("GET /api/v1/users/{userId}")
    @Nested
    class GetUser {
        
        @DisplayName("내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.")
        @Test
        void returnsUserInfo_whenUserExists() {
            // arrange
            UserModel savedUser = userJpaRepository.save(
                new UserModel("user123", "test@example.com", "1990-01-01", "M")
            );
            String requestUrl = ENDPOINT_GET_USER.apply("user123");

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().id()).isEqualTo("user123"),
                () -> assertThat(response.getBody().data().email()).isEqualTo("test@example.com"),
                () -> assertThat(response.getBody().data().birthDate().toString()).isEqualTo("1990-01-01"),
                () -> assertThat(response.getBody().data().gender()).isEqualTo("M"),
                () -> assertThat(response.getBody().data().point()).isEqualTo(0)
            );
        }

        @DisplayName("존재하지 않는 ID로 조회할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void returnsNotFound_whenUserDoesNotExist() {
            // arrange
            String requestUrl = ENDPOINT_GET_USER.apply("nonexistent");

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }

    @DisplayName("GET /api/v1/users/{userId}/points")
    @Nested
    class GetUserPoint {
        
        @DisplayName("포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.")
        @Test
        void returnsUserPoint_whenUserExists() {
            // arrange
            UserModel savedUser = userJpaRepository.save(
                new UserModel("user123", "test@example.com", "1990-01-01", "M")
            );
            String requestUrl = ENDPOINT_GET_POINT.apply("user123");
            
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "user123");
            HttpEntity<String> httpEntity = new HttpEntity<>(null, headers);

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.PointResponse>> response =
                testRestTemplate.exchange(requestUrl, HttpMethod.GET, httpEntity, responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().point()).isEqualTo(0)
            );
        }

        @DisplayName("X-USER-ID 헤더가 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenXUserIdHeaderIsMissing() {
            // arrange
            String requestUrl = ENDPOINT_GET_POINT.apply("user123");

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.PointResponse>> response =
                testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }

    @DisplayName("POST /api/v1/users/{userId}/points/charge")
    @Nested
    class ChargePoint {
        
        @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
        @Test
        void returnsChargedPoint_whenValidUserChargesPoint() {
            // arrange
            UserModel savedUser = userJpaRepository.save(
                new UserModel("user123", "test@example.com", "1990-01-01", "M")
            );
            String requestUrl = ENDPOINT_CHARGE_POINT.apply("user123");
            
            UserV1Dto.PointChargeRequest request = new UserV1Dto.PointChargeRequest(1000);
            HttpEntity<UserV1Dto.PointChargeRequest> httpEntity = new HttpEntity<>(request);

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.PointResponse>> response =
                testRestTemplate.exchange(requestUrl, HttpMethod.POST, httpEntity, responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().point()).isEqualTo(1000)
            );
        }

        @DisplayName("존재하지 않는 유저로 요청할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void returnsNotFound_whenUserDoesNotExist() {
            // arrange
            String requestUrl = ENDPOINT_CHARGE_POINT.apply("nonexistent");
            
            UserV1Dto.PointChargeRequest request = new UserV1Dto.PointChargeRequest(1000);
            HttpEntity<UserV1Dto.PointChargeRequest> httpEntity = new HttpEntity<>(request);

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.PointResponse>> response =
                testRestTemplate.exchange(requestUrl, HttpMethod.POST, httpEntity, responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }
}
