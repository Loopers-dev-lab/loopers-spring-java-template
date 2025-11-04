package com.loopers.interfaces.api.user;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest {

    private static final String ENDPOINT_SIGNUP = "/api/v1/users";

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

    @DisplayName("POST /api/v1/users - 회원가입")
    @Nested
    class Signup {

        @DisplayName("회원가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        @Test
        void returnsCreatedUserInfo_whenSignupIsSuccessful() {
            // arrange
            UserV1Dto.SignupRequest request = new UserV1Dto.SignupRequest(
                    "testId123",
                    "test@test.com",
                    "2000-03-29",
                    "F"
            );

            // act
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response = testRestTemplate.exchange(
                    ENDPOINT_SIGNUP,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);

            UserV1Dto.UserResponse userResponse = response.getBody().data();
            assertAll(
                    () -> assertThat(userResponse.id()).isNotNull(),
                    () -> assertThat(userResponse.loginId()).isEqualTo("testId123"),
                    () -> assertThat(userResponse.email()).isEqualTo("test@test.com"),
                    () -> assertThat(userResponse.birthDate()).isEqualTo("2000-03-29"),
                    () -> assertThat(userResponse.gender()).isEqualTo("F")
            );

            assertThat(userJpaRepository.count()).isEqualTo(1);
        }

        @DisplayName("회원가입 시에 성별이 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returns400_whenGenderIsMissing() {
            // arrange
            String requestBody = """
                    {
                        "loginId": "testId123",
                        "email": "test@test.com",
                        "birthDate": "2000-03-29"
                    }
                    """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_SIGNUP,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(userJpaRepository.count()).isEqualTo(0);
        }

        @DisplayName("회원가입 시에 로그인 ID가 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returns400_whenLoginIdIsMissing() {
            // arrange
            String requestBody = """
                    {
                        "email": "test@test.com",
                        "birthDate": "2000-03-29",
                        "gender": "M"
                    }
                    """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_SIGNUP,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(userJpaRepository.count()).isEqualTo(0);
        }

        @DisplayName("회원가입 시에 이메일이 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returns400_whenEmailIsMissing() {
            // arrange
            String requestBody = """
                    {
                        "loginId": "testId123",
                        "birthDate": "2000-03-29",
                        "gender": "M"
                    }
                    """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_SIGNUP,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(userJpaRepository.count()).isEqualTo(0);
        }

        @DisplayName("회원가입 시에 생년월일이 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returns400_whenBirthDateIsMissing() {
            // arrange
            String requestBody = """
                    {
                        "loginId": "testId123",
                        "email": "test@test.com",
                        "gender": "M"
                    }
                    """;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_SIGNUP,
                    HttpMethod.POST,
                    new HttpEntity<>(requestBody, headers),
                    String.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(userJpaRepository.count()).isEqualTo(0);
        }

        @DisplayName("회원가입 시에 이메일 형식이 올바르지 않을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returns400_whenEmailFormatIsInvalid() {
            // arrange
            UserV1Dto.SignupRequest request = new UserV1Dto.SignupRequest(
                    "testId123",
                    "invalidemail",
                    "2000-03-29",
                    "M"
            );

            // act
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_SIGNUP,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    String.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(userJpaRepository.count()).isEqualTo(0);
        }

        @DisplayName("회원가입 시에 로그인 ID가 형식에 맞지 않을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returns400_whenLoginIdFormatIsInvalid() {
            // arrange
            UserV1Dto.SignupRequest request = new UserV1Dto.SignupRequest(
                    "한글아이디",
                    "test@test.com",
                    "2000-03-29",
                    "F"
            );

            // act
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_SIGNUP,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    String.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(userJpaRepository.count()).isEqualTo(0);
        }

        @DisplayName("회원가입 시에 생년월일 형식이 올바르지 않을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returns400_whenBirthDateFormatIsInvalid() {
            // arrange
            UserV1Dto.SignupRequest request = new UserV1Dto.SignupRequest(
                    "testId123",
                    "test@test.com",
                    "2000/03/29",
                    "F"
            );

            // act
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_SIGNUP,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    String.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(userJpaRepository.count()).isEqualTo(0);
        }

        @DisplayName("회원가입 시에 유효하지 않은 성별 코드가 주어지면, 400 Bad Request 응답을 반환한다.")
        @Test
        void returns400_whenGenderCodeIsInvalid() {
            // arrange
            UserV1Dto.SignupRequest request = new UserV1Dto.SignupRequest(
                    "testId123",
                    "test@test.com",
                    "2000-03-29",
                    "FEMALE"
            );

            // act
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_SIGNUP,
                    HttpMethod.POST,
                    new HttpEntity<>(request),
                    String.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(userJpaRepository.count()).isEqualTo(0);
        }

        @DisplayName("이미 가입된 로그인 ID로 회원가입 시도 시, 409 Conflict 응답을 반환한다.")
        @Test
        void returns409_whenLoginIdAlreadyExists() {
            // arrange
            UserV1Dto.SignupRequest firstRequest = new UserV1Dto.SignupRequest(
                    "testId123",
                    "test1@test.com",
                    "2000-03-29",
                    "M"
            );
            testRestTemplate.exchange(
                    ENDPOINT_SIGNUP,
                    HttpMethod.POST,
                    new HttpEntity<>(firstRequest),
                    new ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            );

            UserV1Dto.SignupRequest duplicateRequest = new UserV1Dto.SignupRequest(
                    "testId123",
                    "test2@test.com",
                    "2000-01-23",
                    "F"
            );

            // act
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_SIGNUP,
                    HttpMethod.POST,
                    new HttpEntity<>(duplicateRequest),
                    String.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(userJpaRepository.count()).isEqualTo(1);
        }

        @DisplayName("이미 가입된 이메일로 회원가입 시도 시, 409 Conflict 응답을 반환한다.")
        @Test
        void returns409_whenEmailAlreadyExists() {
            // arrange
            UserV1Dto.SignupRequest firstRequest = new UserV1Dto.SignupRequest(
                    "testId123",
                    "test@test.com",
                    "2000-03-29",
                    "M"
            );
            testRestTemplate.exchange(
                    ENDPOINT_SIGNUP,
                    HttpMethod.POST,
                    new HttpEntity<>(firstRequest),
                    new ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            );

            UserV1Dto.SignupRequest duplicateRequest = new UserV1Dto.SignupRequest(
                    "testId456",
                    "test@test.com",
                    "2000-01-23",
                    "F"
            );

            // act
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_SIGNUP,
                    HttpMethod.POST,
                    new HttpEntity<>(duplicateRequest),
                    String.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            assertThat(userJpaRepository.count()).isEqualTo(1);
        }
    }

    @DisplayName("GET /api/v1/users - 내 정보 조회")
    @Nested
    class GetUserInfo {

        private static final String ENDPOINT_GET_USER_INFO = "/api/v1/users";

        @DisplayName("내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.")
        @Test
        void returnsUserInfo_whenUserExists() {
            // arrange
            UserV1Dto.SignupRequest signupRequest = new UserV1Dto.SignupRequest(
                    "testId123",
                    "test@test.com",
                    "2000-03-29",
                    "F"
            );
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> signupResponse = testRestTemplate.exchange(
                    ENDPOINT_SIGNUP,
                    HttpMethod.POST,
                    new HttpEntity<>(signupRequest),
                    new ParameterizedTypeReference<>() {}
            );
            assertNotNull(signupResponse.getBody());
            Long userId = signupResponse.getBody().data().id();

            String requestUrl = ENDPOINT_GET_USER_INFO + "?loginId=testId123";

            // act
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response = testRestTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);

            UserV1Dto.UserResponse userResponse = response.getBody().data();
            assertAll(
                    () -> assertThat(userResponse.id()).isEqualTo(userId),
                    () -> assertThat(userResponse.loginId()).isEqualTo("testId123"),
                    () -> assertThat(userResponse.email()).isEqualTo("test@test.com"),
                    () -> assertThat(userResponse.birthDate()).isEqualTo("2000-03-29"),
                    () -> assertThat(userResponse.gender()).isEqualTo("F")
            );
        }

        @DisplayName("존재하지 않는 ID로 조회할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void returns404_whenUserDoesNotExist() {
            // arrange
            String requestUrl = ENDPOINT_GET_USER_INFO + "?loginId=nonExistent";

            // act
            ResponseEntity<String> response = testRestTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    null,
                    String.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
