package com.loopers.interfaces.api;

import static com.loopers.fixtures.UserTestFixture.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.Objects;

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

import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserRepository;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.interfaces.api.user.UserV1Dtos;
import com.loopers.support.Uris;
import com.loopers.utils.DatabaseCleanUp;

/**
 * @author hyunjikoh
 * @since 2025. 10. 27.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("User API E2E 테스트")
class UserV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    private UserRepository userRepository;

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

    @Nested
    @DisplayName("회원가입 API")
    class RegisterUserTest {

        @Test
        @DisplayName("올바른 API 요청으로 회원가입하면 생성된 사용자 정보를 응답한다")
        void register_user_success() {
            // given
            UserV1Dtos.UserRegisterRequest apiRequest = UserTestFixture.createDefaultApiRequest();

            // when
            ParameterizedTypeReference<ApiResponse<UserV1Dtos.UserRegisterResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<UserV1Dtos.UserRegisterResponse>> response =
                    testRestTemplate.exchange(Uris.User.REGISTER, HttpMethod.POST,
                            new HttpEntity<>(apiRequest), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().username())
                            .isEqualTo(DEFAULT_USERNAME),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().email())
                            .isEqualTo(DEFAULT_EMAIL)
            );
        }

        @Test
        @DisplayName("성별이 null인 회원가입 요청 시 400 Bad Request 응답을 반환한다")
        void register_user_fail_when_gender_is_null() {
            // given
            UserV1Dtos.UserRegisterRequest apiRequest = UserTestFixture.createApiRequest(
                    "testuser", "dvum0045@gmail.com", "1990-01-01", null);

            // when
            ParameterizedTypeReference<ApiResponse<UserV1Dtos.UserRegisterResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<UserV1Dtos.UserRegisterResponse>> response =
                    testRestTemplate.exchange(Uris.User.REGISTER, HttpMethod.POST,
                            new HttpEntity<>(apiRequest), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }

    @Nested
    @DisplayName("사용자 조회 API")
    class GetUserTest {

        @Test
        @DisplayName("등록된 사용자명으로 사용자를 조회하면 사용자 정보를 응답한다")
        void get_user_by_username_success() {
            // given
            UserEntity userEntity = UserTestFixture.createDefaultUserEntity();
            userRepository.save(userEntity);

            // when
            ParameterizedTypeReference<ApiResponse<UserV1Dtos.UserInfoResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<UserV1Dtos.UserInfoResponse>> response =
                    testRestTemplate.exchange(Uris.User.GET_BY_USERNAME + "?username=testuser",
                            HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().username())
                            .isEqualTo(DEFAULT_USERNAME),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().email())
                            .isEqualTo(DEFAULT_EMAIL),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().gender())
                            .isEqualTo(DEFAULT_GENDER)
            );
        }

        @Test
        @DisplayName("존재하지 않는 사용자명으로 조회하면 404 Not Found 응답을 반환한다")
        void get_user_by_username_fail_when_not_found() {
            // given
            String nonExistentUsername = "nonExistentUser";

            // when
            ParameterizedTypeReference<ApiResponse<UserV1Dtos.UserInfoResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<UserV1Dtos.UserInfoResponse>> response =
                    testRestTemplate.exchange(Uris.User.GET_BY_USERNAME + "?username=" + nonExistentUsername,
                            HttpMethod.GET, null, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }
}
