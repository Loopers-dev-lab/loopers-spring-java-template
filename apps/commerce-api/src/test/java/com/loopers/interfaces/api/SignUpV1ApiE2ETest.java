package com.loopers.interfaces.api;

import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.domain.user.Gender;
import com.loopers.interfaces.api.signup.SignUpV1Dto;
import com.loopers.domain.user.UserTestFixture;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class SignUpV1ApiE2ETest {

    private static final String ENDPOINT_SIGNUP = "/api/v1/signup";

    private final TestRestTemplate testRestTemplate;
    private final UserJpaRepository userJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public SignUpV1ApiE2ETest(
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

    @DisplayName("POST /api/v1/signup")
    @Nested
    class SignUp {
        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        @ParameterizedTest
        @EnumSource(Gender.class)
        void returnsUserInfo_whenSignUpSucceeds(Gender gender) {
            // arrange
            SignUpV1Dto.SignUpRequest requestBody = new SignUpV1Dto.SignUpRequest(
                UserTestFixture.ValidUser.USER_ID,
                UserTestFixture.ValidUser.EMAIL,
                UserTestFixture.ValidUser.BIRTH_DATE,
                gender.name()
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SignUpV1Dto.SignUpRequest> httpEntity = new HttpEntity<>(requestBody, headers);

            // act
            ParameterizedTypeReference<ApiResponse<SignUpV1Dto.SignupResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<SignUpV1Dto.SignupResponse>> response =
                testRestTemplate.exchange(ENDPOINT_SIGNUP, HttpMethod.POST, httpEntity, responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(userJpaRepository.count()).isEqualTo(1L)
            );
        }

        @DisplayName("회원 가입 시에 성별이 없을 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returns400_whenSignUpWithNoGender() {
            // arrange
            SignUpV1Dto.SignUpRequest requestBody = new SignUpV1Dto.SignUpRequest(
                UserTestFixture.ValidUser.USER_ID,
                UserTestFixture.ValidUser.EMAIL,
                UserTestFixture.ValidUser.BIRTH_DATE,
                null // gender missing
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<SignUpV1Dto.SignUpRequest> httpEntity = new HttpEntity<>(requestBody, headers);

            // act
            ParameterizedTypeReference<ApiResponse<SignUpV1Dto.SignupResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<SignUpV1Dto.SignupResponse>> response =
                testRestTemplate.exchange(ENDPOINT_SIGNUP, HttpMethod.POST, httpEntity, responseType);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode().value()).isEqualTo(400),
                () -> assertThat(userJpaRepository.count()).isEqualTo(0L)
            );
        }
    }
}
