package com.loopers.interfaces.api.members;

import com.loopers.application.members.MemberFacade;
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
import org.springframework.http.*;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MemberV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final MemberFacade memberFacade;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public MemberV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            MemberFacade memberFacade,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.memberFacade = memberFacade;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("회원 가입 (POST /api/v1/members/register)")
    @Nested
    class MemberRegistration {

        @DisplayName("유효한 정보로 가입 시 201과 회원 정보를 반환한다")
        @Test
        void shouldReturn201AndMemberInfo_whenValidDataProvided() {
            MemberV1Dto.MemberRegisterRequest request = new MemberV1Dto.MemberRegisterRequest(
                    "test123", "홍길동", "test@example.com", "password", "1990-01-01", "MALE"
            );

            ParameterizedTypeReference<ApiResponse<MemberV1Dto.MemberResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<MemberV1Dto.MemberResponse>> response =
                    testRestTemplate.exchange("/api/v1/members/register", HttpMethod.POST,
                            new HttpEntity<>(request), responseType);

            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().memberId()).isEqualTo("test123"),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().name()).isEqualTo("홍길동"),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().email()).isEqualTo("test@example.com"),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().gender()).isEqualTo("MALE")
            );
        }

        @DisplayName("성별이 누락된 경우 400 Bad Request를 반환한다")
        @Test
        void shouldReturn400_whenGenderIsMissing() {
            MemberV1Dto.MemberRegisterRequest request = new MemberV1Dto.MemberRegisterRequest(
                    "test123", "홍길동", "test@example.com", "password", "1990-01-01", null
            );

            ParameterizedTypeReference<ApiResponse<MemberV1Dto.MemberResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<MemberV1Dto.MemberResponse>> response =
                    testRestTemplate.exchange("/api/v1/members/register", HttpMethod.POST,
                            new HttpEntity<>(request), responseType);

            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }

    @DisplayName("내 정보 조회 (GET /api/v1/members/me)")
    @Nested
    class MyInfoRetrieval {

        @DisplayName("유효한 사용자 ID로 조회 시 200과 회원 정보를 반환한다")
        @Test
        void shouldReturn200AndMemberInfo_whenValidUserIdProvided() {
            memberFacade.registerMember("test123", "홍길동", "test@example.com", "password", "1990-01-01", "MALE");

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "test123");

            ParameterizedTypeReference<ApiResponse<MemberV1Dto.MemberResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<MemberV1Dto.MemberResponse>> response =
                    testRestTemplate.exchange("/api/v1/members/me", HttpMethod.GET,
                            new HttpEntity<>(null, headers), responseType);

            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().memberId()).isEqualTo("test123"),
                    () -> assertThat(response.getBody().data().name()).isEqualTo("홍길동"),
                    () -> assertThat(response.getBody().data().email()).isEqualTo("test@example.com")
            );
        }

        @DisplayName("존재하지 않는 사용자 ID로 조회 시 404를 반환한다")
        @Test
        void shouldReturn404_whenUserNotFound() {
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "nonexistent");

            ParameterizedTypeReference<ApiResponse<MemberV1Dto.MemberResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<MemberV1Dto.MemberResponse>> response =
                    testRestTemplate.exchange("/api/v1/members/me", HttpMethod.GET,
                            new HttpEntity<>(null, headers), responseType);

            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }

}
