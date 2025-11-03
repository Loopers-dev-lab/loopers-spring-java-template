package com.loopers.interfaces.api;


import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.user.UserV1Dto;
import com.loopers.testcontainers.MySqlTestContainersConfig;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestContainersConfig.class)
class UserV1ApiE2ETest {

    private static final String ENDPOINT_REGISTER = "/api/v1/users";
    private static final Function<String, String> ENDPOINT_GET = userId -> "/api/v1/users/" + userId;

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
    class RegisterUser {

        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        @Test
        void returnsUserInfo_whenRegistrationIsSuccessful() {
            // given
            UserV1Dto.RegisterRequest request = new UserV1Dto.RegisterRequest(
                    "testuser1",
                    "test@example.com",
                    "1990-01-01",
                    "MALE"
            );

            // when
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, new HttpEntity<>(request), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().id()).isEqualTo("testuser1"),
                    () -> assertThat(response.getBody().data().email()).isEqualTo("test@example.com"),
                    () -> assertThat(response.getBody().data().birthDate()).isEqualTo("1990-01-01"),
                    () -> assertThat(response.getBody().data().gender()).isEqualTo("MALE")
            );

            // DB에 실제로 저장되었는지 확인
            assertTrue(userJpaRepository.existsById("testuser1"));
        }

        @DisplayName("회원 가입 시에 성별이 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenGenderIsMissing() {
            // given
            String requestBody = """
                    {
                        "id": "testuser1",
                        "email": "test@example.com",
                        "birthDate": "1990-01-01"
                    }
                    """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            // when
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, requestEntity, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );

            // DB에 저장되지 않았는지 확인
            assertThat(userJpaRepository.existsById("testuser1")).isFalse();
        }

        @DisplayName("회원 가입 시에 성별이 MALE 또는 FEMALE이 아닐 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenGenderIsInvalid() {
            // given
            UserV1Dto.RegisterRequest request = new UserV1Dto.RegisterRequest(
                    "testuser1",
                    "test@example.com",
                    "1990-01-01",
                    "UNKNOWN"
            );

            // when
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, new HttpEntity<>(request), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );

            // DB에 저장되지 않았는지 확인
            assertThat(userJpaRepository.existsById("testuser1")).isFalse();
        }

        @DisplayName("이미 존재하는 ID로 회원가입 시도 시, 409 Conflict 응답을 반환한다.")
        @Test
        void returnsConflict_whenUserIdAlreadyExists() {
            // given
            UserV1Dto.RegisterRequest firstRequest = new UserV1Dto.RegisterRequest(
                    "testuser1",
                    "test@example.com",
                    "1990-01-01",
                    "MALE"
            );

            // 첫 번째 회원 가입 (성공)
            testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, new HttpEntity<>(firstRequest), new ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {
            });

            // 같은 ID로 두 번째 회원 가입 시도
            UserV1Dto.RegisterRequest secondRequest = new UserV1Dto.RegisterRequest(
                    "testuser1",
                    "different@example.com",
                    "1995-05-05",
                    "FEMALE"
            );

            // when
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_REGISTER, HttpMethod.POST, new HttpEntity<>(secondRequest), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT)
            );
        }
    }

    @DisplayName("GET /api/v1/users/{userId}")
    @Nested
    class GetUserById {

        @DisplayName("내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.")
        @Test
        void returnsUserInfo_whenUserExists() {
            // given
            String userId = "testuser1";
            String email = "test@example.com";
            String birthDate = "1990-01-01";
            String gender = "MALE";

            // 회원 생성
            userJpaRepository.save(com.loopers.domain.user.User.create(userId, email, birthDate, gender));

            String requestUrl = ENDPOINT_GET.apply(userId);

            // when
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().id()).isEqualTo(userId),
                    () -> assertThat(response.getBody().data().email()).isEqualTo(email),
                    () -> assertThat(response.getBody().data().birthDate()).isEqualTo(birthDate),
                    () -> assertThat(response.getBody().data().gender()).isEqualTo(gender)
            );
        }

        @DisplayName("존재하지 않는 ID로 조회할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void returnsNotFound_whenUserDoesNotExist() {
            // given
            String nonExistentUserId = "nonexistent";
            String requestUrl = ENDPOINT_GET.apply(nonExistentUserId);

            // when
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }
}
