package com.loopers.interfaces.api;

import com.loopers.application.signup.SignUpFacade;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserTestFixture;
import com.loopers.interfaces.api.userinfo.UserInfoV1Dto;
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
public class UserInfoV1ApiE2ETest {

    private static final String ENDPOINT_ME = "/api/v1/me";

    private final TestRestTemplate testRestTemplate;
    private final SignUpFacade signUpFacade;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public UserInfoV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        SignUpFacade signUpFacade,
        DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.signUpFacade = signUpFacade;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/me")
    @Nested
    class GetUserInfo {
        @DisplayName("내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.")
        @ParameterizedTest
        @EnumSource(Gender.class)
        void returnsUserInfo_whenUserExists(Gender gender) {
            // arrange
            String userId = UserTestFixture.ValidUser.USER_ID;
            String email = UserTestFixture.ValidUser.EMAIL;
            String birthDate = UserTestFixture.ValidUser.BIRTH_DATE;
            signUpFacade.signUp(userId, email, birthDate, gender);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // act
            ParameterizedTypeReference<ApiResponse<UserInfoV1Dto.UserInfoResponse>> responseType = new ParameterizedTypeReference<>() {};
            headers.add("X-USER-ID", userId);
            ResponseEntity<ApiResponse<UserInfoV1Dto.UserInfoResponse>> response =
                testRestTemplate.exchange(ENDPOINT_ME, HttpMethod.GET, httpEntity, responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data()).isNotNull(),
                () -> assertThat(response.getBody().data().userId()).isEqualTo(userId),
                () -> assertThat(response.getBody().data().email()).isEqualTo(email),
                () -> assertThat(response.getBody().data().birthDate()).isEqualTo(birthDate),
                () -> assertThat(response.getBody().data().gender()).isEqualTo(gender.name())
            );
        }

        @DisplayName("존재하지 않는 ID 로 조회할 경우, `404 Not Found` 응답을 반환한다.")
        @Test
        void returns404_whenUserDoesNotExist() {
            // arrange
            String userId = "unknown";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            headers.add("X-USER-ID", userId);
            ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(ENDPOINT_ME, HttpMethod.GET, httpEntity, responseType);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode().value()).isEqualTo(404),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().meta()).isNotNull(),
                () -> assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.FAIL)
            );
        }

        @DisplayName("`X-USER-ID` 헤더가 없을 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returns400_whenHeaderMissing() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

            // act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response =
                testRestTemplate.exchange(ENDPOINT_ME, HttpMethod.GET, httpEntity, responseType);

            // assert
            assertAll(
                () -> assertThat(response.getStatusCode().value()).isEqualTo(400),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().meta()).isNotNull(),
                () -> assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.FAIL)
            );
        }
    }
}