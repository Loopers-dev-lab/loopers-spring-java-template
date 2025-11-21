package com.loopers.interfaces.api;

import com.loopers.domain.user.Email;
import com.loopers.domain.user.UserId;
import com.loopers.domain.user.BirthDate;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserRepository;
import com.loopers.domain.user.Gender;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest {

    private static final String ENDPOINT_SIGNUP = "/api/v1/users/signup";
    private static final Function<String, String> ENDPOINT_GET = userId -> "/api/v1/users/" + userId;

    private final TestRestTemplate testRestTemplate;
    private final UserRepository userRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public UserV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        UserRepository userRepository,
        DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.userRepository = userRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    /*
    회원가입
    - [x]  회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.
    - [x]  회원 가입 시에 필수 필드가 없을 경우, `400 Bad Request` 응답을 반환한다.

    내 정보 조회
    - [x]  내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.
    - [x]  존재하지 않는 ID 로 조회할 경우, `404 Not Found` 응답을 반환한다.
     */

    @DisplayName("POST /api/v1/users/signup")
    @Nested
    class Signup {
        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        @Test
        void returnsUserInfo_whenSignupIsSuccessful() {
            // arrange
            UserV1Dto.SignupRequest request = new UserV1Dto.SignupRequest(
                "user123",
                "user123@example.com",
                "male",
                "1999-01-01"
            );

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                testRestTemplate.exchange(ENDPOINT_SIGNUP, HttpMethod.POST, new HttpEntity<>(request), responseType);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().userId()).isEqualTo("user123"),
                () -> assertThat(response.getBody().data().email()).isEqualTo("user123@example.com"),
                () -> assertThat(response.getBody().data().birthDate()).isEqualTo("1999-01-01")
            );
        }

        @DisplayName("회원 가입 시에 성별이 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void throwsBadRequest_whenRequiredFieldIsMissing() {
            // arrange - gender 필드를 null로 설정
            UserV1Dto.SignupRequest request = new UserV1Dto.SignupRequest(
                "user123",
                "user123@example.com",
                null,
                "1999-01-01"
            );

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                testRestTemplate.exchange(ENDPOINT_SIGNUP, HttpMethod.POST, new HttpEntity<>(request), responseType);

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
        void returnsUserInfo_whenValidUserIdIsProvided() {
            // arrange
            UserModel userModel = userRepository.save(
                new UserModel(new UserId("user123"), new Email("user123@example.com"), new Gender("male"), new BirthDate("1999-01-01"))
            );
            String requestUrl = ENDPOINT_GET.apply(userModel.getUserId().userId());

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().userId()).isEqualTo(userModel.getUserId().userId()),
                () -> assertThat(response.getBody().data().email()).isEqualTo(userModel.getEmail().email()),
                () -> assertThat(response.getBody().data().birthDate()).isEqualTo(userModel.getBirthDate().birthDate())
            );
        }

        @DisplayName("존재하지 않는 ID 로 조회할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void throwsNotFoundException_whenUserIdDoesNotExist() {
            // arrange
            String invalidUserId = "nonexistent";
            String requestUrl = ENDPOINT_GET.apply(invalidUserId);

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
}
