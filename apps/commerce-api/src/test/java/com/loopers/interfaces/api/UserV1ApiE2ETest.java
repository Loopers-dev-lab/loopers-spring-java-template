package com.loopers.interfaces.api;

import com.loopers.interfaces.api.user.UserV1Dto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserV1ApiE2ETest {

    private final String ENDPOINT_USER = "/api/v1/users";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public UserV1ApiE2ETest(
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

    @DisplayName("POST /api/v1/users")
    @Nested
    class Post {
        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        @Test
        void returnUserInfo_whenRegisterSuccess() {
            // arrange
            UserV1Dto.UserRegisterRequest request = new UserV1Dto.UserRegisterRequest(
                    "user123",
                    "xx@yy.zz",
                    "1993-03-13",
                    "male"
            );

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response = testRestTemplate.exchange(ENDPOINT_USER, HttpMethod.POST, new HttpEntity<>(request), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().id()).isEqualTo(request.id()),
                    () -> assertThat(response.getBody().data().email()).isEqualTo(request.email()),
                    () -> assertThat(response.getBody().data().birthday()).isEqualTo(request.birthday()),
                    () -> assertThat(response.getBody().data().gender()).isEqualTo(request.gender())
            );
        }

        @DisplayName("회원 가입 시에 성별이 없을 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenGenderIsMissing() {
            // arrange
            UserV1Dto.UserRegisterRequest request = new UserV1Dto.UserRegisterRequest(
                    "user123",
                    "xx@yy.zz",
                    "1993-03-13",
                    null
            );

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response = testRestTemplate.exchange(ENDPOINT_USER, HttpMethod.POST, new HttpEntity<>(request), responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }
    }

    @DisplayName("GET /api/v1/users/me")
    @Nested
    class Get {
        private final String validUserId = "user123";
        private final String validEmail = "xx@yy.zz";
        private final String validBirthday = "1993-03-13";
        private final String validGender = "male";

        @BeforeEach
        void setupUser() {
            UserV1Dto.UserRegisterRequest request = new UserV1Dto.UserRegisterRequest(
                    validUserId,
                    validEmail,
                    validBirthday,
                    validGender
            );
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            testRestTemplate.exchange(ENDPOINT_USER, HttpMethod.POST, new HttpEntity<>(request), responseType);
        }

        @DisplayName("로그인한 유저가 내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.")
        @Test
        void returnUserInfo_whenGetUserInfoSuccess() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", validUserId);

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response = testRestTemplate.exchange(ENDPOINT_USER + "/me", HttpMethod.GET, requestEntity, responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().id()).isEqualTo("user123"),
                    () -> assertThat(response.getBody().data().email()).isEqualTo("xx@yy.zz"),
                    () -> assertThat(response.getBody().data().birthday()).isEqualTo("1993-03-13"),
                    () -> assertThat(response.getBody().data().gender()).isEqualTo("male")
            );
        }

        @DisplayName("비로그인 유저가 내 정보 조회를 시도할 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsMissing() {
            // arrange

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<Void> requestEntity = new HttpEntity<>(null, null);
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response = testRestTemplate.exchange(ENDPOINT_USER + "/me", HttpMethod.GET, requestEntity, responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("X-USER-ID 헤더가 빈 문자열일 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsEmpty() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", "");

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response = testRestTemplate.exchange(ENDPOINT_USER + "/me", HttpMethod.GET, requestEntity, responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("X-USER-ID 헤더가 공백만 있을 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsBlank() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", "   ");

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response = testRestTemplate.exchange(ENDPOINT_USER + "/me", HttpMethod.GET, requestEntity, responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("존재하지 않는 ID로 조회할 경우, `404 Not Found` 응답을 반환한다.")
        @Test
        void returnNotFound_whenUserIdDoesNotExist() {
            // arrange
            String invalidUserId = "nonexist";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", invalidUserId);

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response = testRestTemplate.exchange(ENDPOINT_USER + "/me", HttpMethod.GET, requestEntity, responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }
    }
}
