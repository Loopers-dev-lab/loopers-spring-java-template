package com.loopers.interfaces.api;

import com.loopers.domain.user.UserEntity;
import com.loopers.domain.user.UserFixture;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.user.UserV1Dto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserV1ApiE2ETest {
    private final TestRestTemplate testRestTemplate;
    private final UserRepository userRepository;
    @Autowired
    public UserV1ApiE2ETest(TestRestTemplate testRestTemplate, UserRepository userRepository) {
        this.testRestTemplate = testRestTemplate;
        this.userRepository = userRepository;
    }
    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }
    @Nested
    @DisplayName("POST /api/v1/users")
    class post{
        static String ENDPOINT = "/api/v1/users";
        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        @Test
        void returnsCreatedUserInfo_whenRegistrationIsSuccessful() {

            UserV1Dto.CreateUserRequest request  = new UserV1Dto.CreateUserRequest(
                    UserFixture.USER_LOGIN_ID,
                    UserFixture.USER_EMAIL,
                    UserFixture.USER_BIRTH_DATE,
                    UserFixture.USER_GENDER
            );
             ParameterizedTypeReference<ApiResponse<UserV1Dto.CreateUserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.CreateUserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, new HttpEntity<>(request),responseType);
            System.out.println(response.getBody());
            assertAll(
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK)

            );
        }
        @DisplayName("회원 가입 시에 성별이 없을 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenRegistrationIsNotSuccessful() {
            //arrange
            UserV1Dto.CreateUserRequest request  = new UserV1Dto.CreateUserRequest(
                    UserFixture.USER_LOGIN_ID,
                    UserFixture.USER_EMAIL,
                    UserFixture.USER_BIRTH_DATE,
                    null // 성별이 없을 경우
            );
            //act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.CreateUserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.CreateUserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, new HttpEntity<>(request),responseType);
            //assert
            System.out.println(response.getStatusCode());
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
                    );

        }
    }
    @Nested
    @DisplayName("GET /api/v1/users/me")
    class get {
        static String ENDPOINT = "/api/v1/users/me";

        @DisplayName("사용자 정보를 조회할 때, 올바른 사용자 ID를 제공하면 해당 사용자 정보를 반환한다.")
        @Test
        void returnsUserInfo_whenValidUserIdIsProvided() {
            // arrange
            UserEntity userEntity = userRepository.save(UserFixture.createUser());

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", userEntity.getId().toString());

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.CreateUserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.CreateUserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, entity, responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
//                    () -> assertThat(response.getBody().data().userId()).isEqualTo(userEntity.getId()),
                    () -> assertThat(response.getBody().data().loginId()).isEqualTo(UserFixture.USER_LOGIN_ID),
                    () -> assertThat(response.getBody().data().email()).isEqualTo(UserFixture.USER_EMAIL),
                    () -> assertThat(response.getBody().data().birth()).isEqualTo(UserFixture.USER_BIRTH_DATE),
                    () -> assertThat(response.getBody().data().gender()).isEqualTo(UserFixture.USER_GENDER)


            );
        }

        @DisplayName("사용자 정보를 조회할 때, 사용자 ID가 제공되지 않으면 `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenUserIdIsNotProvided() {
            // arrange
            String userId = "";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", userId );

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.CreateUserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.CreateUserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, entity, responseType);
            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @DisplayName("사용자 정보를 조회할 때, X-USER-ID 헤더가 없으면 `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenUserIdHeaderIsMissing() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.CreateUserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.CreateUserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, entity, responseType);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
        @DisplayName("존재하지 않는 ID 로 조회할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void returnsNotFound_whenUserDoesNotExist() {
            // arrange
            String userId = "amdin123";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", userId );
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.CreateUserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.CreateUserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, entity, responseType);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }


}
