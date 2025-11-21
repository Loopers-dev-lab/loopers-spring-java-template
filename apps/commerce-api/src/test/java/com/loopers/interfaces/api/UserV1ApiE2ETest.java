package com.loopers.interfaces.api;

import com.loopers.domain.user.User;
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
import org.springframework.http.*;

import static com.loopers.support.fixture.UserFixtures.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest {

    private static final String ENDPOINT_GET = "/api/v1/users/me";
    private static final String ENDPOINT_POST = "/api/v1/users/signup";

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

    @DisplayName("POST /api/v1/users/signup")
    @Nested
    class Signup {
        @DisplayName("회원가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        @Test
        void returnsUserInfo_whenSignupSucceeds() {
            // arrange
            UserV1Dto.SignupRequest requestDto = new UserV1Dto.SignupRequest(
                    validLoginId(),
                    validPassword(),
                    validEmail(),
                    validBirthDate(),
                    validGender()
            );

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                testRestTemplate.exchange(ENDPOINT_POST, HttpMethod.POST, new HttpEntity<>(requestDto), responseType);

            // assert
            assertThat(response.getBody()).isNotNull();
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().loginId()).isEqualTo(validLoginId()),
                () -> assertThat(response.getBody().data().email()).isEqualTo(validEmail()),
                () -> assertThat(response.getBody().data().birthDate()).isEqualTo(validBirthDate()),
                () -> assertThat(response.getBody().data().gender()).isEqualTo(validGender())
            );
        }

        @DisplayName("회원가입 시에 성별이 없을 경우, 400 BAD_REQUEST 응답을 반환한다.")
        @Test
        void throwsBadRequest_whenGenderIsNotProvided() {
            // arrange
            UserV1Dto.SignupRequest requestDto = new UserV1Dto.SignupRequest(
                    validLoginId(),
                    validPassword(),
                    validEmail(),
                    validBirthDate(),
                    null
            );

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                testRestTemplate.exchange(ENDPOINT_POST, HttpMethod.POST, new HttpEntity<>(requestDto), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }

    @DisplayName("GET /api/v1/users/me")
    @Nested
    class GetMe {
        @DisplayName("내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.")
        @Test
        void returnsUserInfo_whenGetMeSucceeds() {
            // arrange
            User user = userJpaRepository.save(
                    User.create(validLoginId(), validPassword(), validEmail(), validBirthDate(), validGender())
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", String.valueOf(user.getId()));

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_GET, HttpMethod.GET, new HttpEntity<>(headers), responseType);

            // assert
            assertThat(response.getBody()).isNotNull();
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().loginId()).isEqualTo(validLoginId()),
                    () -> assertThat(response.getBody().data().email()).isEqualTo(validEmail()),
                    () -> assertThat(response.getBody().data().birthDate()).isEqualTo(validBirthDate()),
                    () -> assertThat(response.getBody().data().gender()).isEqualTo(validGender())
            );
        }

        @DisplayName("존재하지 않는 ID 로 조회할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void throwsNotFound_whenInvalidIdIsProvided() {

            Long invalidId = -1L;
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", String.valueOf(invalidId));

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_GET, HttpMethod.GET, new HttpEntity<>(headers), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }
}
