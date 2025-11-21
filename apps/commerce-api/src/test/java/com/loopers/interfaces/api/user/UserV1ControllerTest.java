package com.loopers.interfaces.api.user;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ControllerTest {

    private static final String USER_REGISTER_ENDPOINT = "/api/v1/users/register";
    private static final Function<String, String> GET_USER_ENDPOINT = id -> "/api/v1/users/" + id;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UserJpaRepository userJpaRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/users")
    @Nested
    class RegisterUser {
        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        @Test
        void registerUser_whenSuccessResponseUser() {
            //given
            String userId = "yh45g";
            String email = "yh45g@loopers.com";
            String birth = "1994-12-05";
            String gender = "MALE";

            UserV1Dto.RegisterRequest request = new UserV1Dto.RegisterRequest(userId, email, birth, gender);

            //when
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(USER_REGISTER_ENDPOINT, HttpMethod.POST, new HttpEntity<>(request), responseType);

            //then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo(userId),
                    () -> assertThat(response.getBody().data().email()).isEqualTo(email),
                    () -> assertThat(response.getBody().data().birth()).isEqualTo(birth),
                    () -> assertThat(response.getBody().data().gender()).isEqualTo(gender)
            );
        }
        @DisplayName("회원 가입 시에 성별이 없을 경우, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        void throwsBadRequest_whenGenderIsNotProvided() {
            //given
            String userId = "yh45g";
            String email = "yh45g@loopers.com";
            String birth = "1994-12-05";
            String gender =  null;

            UserV1Dto.RegisterRequest request = new UserV1Dto.RegisterRequest(userId, email, birth, gender);

            //when
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(USER_REGISTER_ENDPOINT, HttpMethod.POST, new HttpEntity<>(request), responseType);

            //then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }

    @DisplayName("GET /api/v1/users/{userId}")
    @Nested
    class GetUserById {
        @DisplayName("내 정보 조회에 성공할 경우, 해당하는 유저 정보를 반환한다.")
        @Test
        void getUserById_whenSuccessResponseUser() {
            //given
            String userId = "yh45g";
            String email = "yh45g@loopers.com";
            String birth = "1994-12-05";
            String gender = "MALE";

            userJpaRepository.save(new User(userId, email, birth, gender));

            String requestUrl = GET_USER_ENDPOINT.apply(userId);

            //when
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            //then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo(userId),
                    () -> assertThat(response.getBody().data().email()).isEqualTo(email),
                    () -> assertThat(response.getBody().data().birth()).isEqualTo(birth),
                    () -> assertThat(response.getBody().data().gender()).isEqualTo(gender)
            );
        }

        @DisplayName("존재하지 않는 ID 로 조회할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void throwsException_whenInvalidUserIdIsProvided() {
            //given
            String userId = "notUserId";
            String requestUrl = GET_USER_ENDPOINT.apply(userId);

            //when
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            //then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }
}
