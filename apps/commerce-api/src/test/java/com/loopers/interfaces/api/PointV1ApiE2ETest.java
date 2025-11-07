package com.loopers.interfaces.api;

import com.loopers.domain.point.PointModel;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.user.UserModel;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.point.PointV1Dto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PointV1ApiE2ETest {

    private static final String ENDPOINT_GET = "/api/v1/points";
    private static final String ENDPOINT_CHARGE = "/api/v1/points/charge";

    private final TestRestTemplate testRestTemplate;
    private final UserRepository userRepository;
    private final PointRepository pointRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public PointV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        UserRepository userRepository,
        PointRepository pointRepository,
        DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.userRepository = userRepository;
        this.pointRepository = pointRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    /*
    포인트 조회
    - [x]  포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.
    - [x]  `X-USER-ID` 헤더가 없을 경우, `400 Bad Request` 응답을 반환한다.

    포인트 충전
    - [x]  존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.
    - [x]  존재하지 않는 유저로 요청할 경우, `404 Not Found` 응답을 반환한다.
     */

    @DisplayName("GET /api/v1/points")
    @Nested
    class GetPoint {
        @DisplayName("포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.")
        @Test
        void returnsPoint_whenValidUserIdHeaderIsProvided() {
            // arrange
            UserModel user = userRepository.save(
                new UserModel("user123", "user123@example.com", "1999-01-01")
            );
            pointRepository.save(new PointModel(user, 500));

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getUserId());

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
                testRestTemplate.exchange(ENDPOINT_GET, HttpMethod.GET, new HttpEntity<>(headers), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().userId()).isEqualTo(user.getUserId()),
                () -> assertThat(response.getBody().data().point()).isEqualTo(500)
            );
        }

        @DisplayName("`X-USER-ID` 헤더가 없을 경우, `400 Bad Request` 응답을 반환한다.")
        @Test
        void throwsBadRequest_whenUserIdHeaderIsMissing() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            // X-USER-ID 헤더를 의도적으로 설정하지 않음

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
                testRestTemplate.exchange(ENDPOINT_GET, HttpMethod.GET, new HttpEntity<>(headers), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }

    @DisplayName("POST /api/v1/points/charge")
    @Nested
    class ChargePoint {
        @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
        @Test
        void chargesPoint_when1000AmountIsProvided() {
            // arrange
            UserModel user = userRepository.save(
                new UserModel("user123", "user123@example.com", "1999-01-01")
            );
            PointV1Dto.ChargeRequest request = new PointV1Dto.ChargeRequest(1000);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getUserId());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
                testRestTemplate.exchange(ENDPOINT_CHARGE, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody()).isNotNull(),
                () -> assertThat(response.getBody().data().userId()).isEqualTo(user.getUserId()),
                () -> assertThat(response.getBody().data().point()).isEqualTo(1000)
            );
        }

        @DisplayName("존재하지 않는 유저로 요청할 경우, `404 Not Found` 응답을 반환한다.")
        @Test
        void throwsNotFoundException_whenUserDoesNotExist() {
            // arrange
            PointV1Dto.ChargeRequest request = new PointV1Dto.ChargeRequest(1000);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "nonexistent");
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
                testRestTemplate.exchange(ENDPOINT_CHARGE, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }
}
