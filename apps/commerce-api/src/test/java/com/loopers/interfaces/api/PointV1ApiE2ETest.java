package com.loopers.interfaces.api;

import com.loopers.interfaces.api.point.PointV1Dto;
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
public class PointV1ApiE2ETest {
    private final String ENDPOINT_USER = "/api/v1/users";
    private final String ENDPOINT_POINT = "/api/v1/points";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public PointV1ApiE2ETest(
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

    @DisplayName("GET /api/v1/points")
    @Nested
    class GetPoints {
        @DisplayName("로그인한 유저가 포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.")
        @Test
        void returnUserPoints_whenGetUserPointsSuccess() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", validUserId);

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(ENDPOINT_POINT, HttpMethod.GET, requestEntity, responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().currentPoint()).isEqualTo(0L)
            );
        }

        @DisplayName("비로그인 유저가 포인트 조회를 시도할 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsMissing() {
            // arrange

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<Void> requestEntity = new HttpEntity<>(null, null);
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(ENDPOINT_POINT, HttpMethod.GET, requestEntity, responseType);

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
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(ENDPOINT_POINT, HttpMethod.GET, requestEntity, responseType);

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
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(ENDPOINT_POINT, HttpMethod.GET, requestEntity, responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("존재하지 않는 유저 ID로 조회할 경우, `404 Not Found` 응답을 반환한다.")
        @Test
        void returnNotFound_whenUserIdDoesNotExist() {
            // arrange
            String invalidUserId = "nonexist";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", invalidUserId);

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<Void> requestEntity = new HttpEntity<>(null, headers);
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(ENDPOINT_POINT, HttpMethod.GET, requestEntity, responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }
    }

    @DisplayName("POST /api/v1/points/charge")
    @Nested
    class ChargePoints {
        @DisplayName("로그인한 유저가 포인트 충전에 성공할 경우, 충전된 보유 총량을 응답으로 반환한다.")
        @Test
        void returnChargedPoints_whenChargeUserPointsSuccess() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", validUserId);
            PointV1Dto.PointChargeRequest request = new PointV1Dto.PointChargeRequest(1000);

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<PointV1Dto.PointChargeRequest> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(ENDPOINT_POINT + "/charge", HttpMethod.POST, requestEntity, responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().currentPoint()).isEqualTo(1000L)
            );
        }

        @DisplayName("비로그인 유저가 포인트 충전을 시도할 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsMissing() {
            // arrange
            PointV1Dto.PointChargeRequest request = new PointV1Dto.PointChargeRequest(1000);

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<PointV1Dto.PointChargeRequest> requestEntity = new HttpEntity<>(request, null);
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(ENDPOINT_POINT + "/charge", HttpMethod.POST, requestEntity, responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("X-USER-ID 헤더가 빈 문자열일 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsEmpty() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", "");
            PointV1Dto.PointChargeRequest request = new PointV1Dto.PointChargeRequest(1000);

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<PointV1Dto.PointChargeRequest> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(ENDPOINT_POINT + "/charge", HttpMethod.POST, requestEntity, responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("X-USER-ID 헤더가 공백만 있을 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsBlank() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", "   ");
            PointV1Dto.PointChargeRequest request = new PointV1Dto.PointChargeRequest(1000);

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<PointV1Dto.PointChargeRequest> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(ENDPOINT_POINT + "/charge", HttpMethod.POST, requestEntity, responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("존재하지 않는 유저로 요청할 경우, `404 Not Found` 응답을 반환한다.")
        @Test
        void returnNotFound_whenChargePointsForNonExistentUser() {
            // arrange
            String invalidUserId = "nonexist";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", invalidUserId);
            PointV1Dto.PointChargeRequest request = new PointV1Dto.PointChargeRequest(1000);

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<PointV1Dto.PointChargeRequest> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(ENDPOINT_POINT + "/charge", HttpMethod.POST, requestEntity, responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }
    }
}
