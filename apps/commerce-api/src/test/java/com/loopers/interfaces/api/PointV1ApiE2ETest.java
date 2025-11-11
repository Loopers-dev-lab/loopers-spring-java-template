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

    @DisplayName("GET /api/v1/points")
    @Nested
    class GetPoints {
        private final String validUserId = "user123";
        private final String validEmail = "xx@yy.zz";
        private final String validBirthday = "1993-03-13";
        private final String validGender = "male";

        // 회원가입 정보 작성
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

        @DisplayName("포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.")
        @Test
        void returnUserPoints_whenGetUserPointsSuccess() {
            // arrange: setupUser() 참조
            String xUserIdHeader = "user123";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", xUserIdHeader);

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

        //`X-USER-ID` 헤더가 없을 경우, `400 Bad Request` 응답을 반환한다.
        @DisplayName("`X-USER-ID` 헤더가 없을 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void returnBadRequest_whenXUserIdHeaderIsMissing() {
            // arrange: setupUser() 참조

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<Void> requestEntity = new HttpEntity<>(null, null);
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(ENDPOINT_POINT, HttpMethod.GET, requestEntity, responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(400);
        }

        @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
        @Test
        void returnChargedPoints_whenChargeUserPointsSuccess() {
            // arrange: setupUser() 참조
            String xUserIdHeader = "user123";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", xUserIdHeader);
            PointV1Dto.PointChargeRequest request = new PointV1Dto.PointChargeRequest(1000);

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<PointV1Dto.PointChargeRequest> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(ENDPOINT_POINT, HttpMethod.POST, requestEntity, responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().currentPoint()).isEqualTo(1000L)
            );
        }

        //존재하지 않는 유저로 요청할 경우, `404 Not Found` 응답을 반환한다.
        @DisplayName("존재하지 않는 유저로 요청할 경우, `404 Not Found` 응답을 반환한다.")
        @Test
        void returnNotFound_whenChargePointsForNonExistentUser() {
            // arrange: setupUser() 참조
            String xUserIdHeader = "nonexist";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", xUserIdHeader);
            PointV1Dto.PointChargeRequest request = new PointV1Dto.PointChargeRequest(1000);

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            HttpEntity<PointV1Dto.PointChargeRequest> requestEntity = new HttpEntity<>(request, headers);
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(ENDPOINT_POINT, HttpMethod.POST, requestEntity, responseType);

            // assert
            assertThat(response.getStatusCode().value()).isEqualTo(404);
        }
    }

}
