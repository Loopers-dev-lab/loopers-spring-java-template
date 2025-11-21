package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.math.BigDecimal;
import java.util.Objects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserRegisterCommand;
import com.loopers.fixtures.UserTestFixture;
import com.loopers.interfaces.api.point.PointV1Dtos;
import com.loopers.support.Uris;
import com.loopers.utils.DatabaseCleanUp;

/**
 * @author hyunjikoh
 * @since 2025. 10. 29.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("Point API E2E 테스트")
class PointV1E2ETest {
    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    private UserFacade userFacade;

    @Autowired
    public PointV1E2ETest(
            TestRestTemplate testRestTemplate,
            DatabaseCleanUp databaseCleanUp,
            UserFacade userFacade
    ) {
        this.testRestTemplate = testRestTemplate;
        this.databaseCleanUp = databaseCleanUp;
        this.userFacade = userFacade;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @Nested
    @DisplayName("포인트 조회 API")
    class GetPointInfoTest {

        @Test
        @DisplayName("X-USER-ID 헤더를 기준으로 사용자의 포인트 조회를 성공한다")
        void get_user_point_success() {
            // given
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            userFacade.registerUser(userCommand);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", userCommand.username());

            // when
            ParameterizedTypeReference<ApiResponse<PointV1Dtos.PointInfoResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PointV1Dtos.PointInfoResponse>> response =
                    testRestTemplate.exchange(Uris.Point.GET_INFO, HttpMethod.GET,
                            new HttpEntity<>(null, headers), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().username())
                            .isEqualTo(userCommand.username()),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().currentPointAmount())
                            .isEqualTo(BigDecimal.ZERO.setScale(2))
            );
        }

        @Test
        @DisplayName("X-USER-ID 헤더가 없을 경우 400 Bad Request 응답을 반환한다")
        void get_user_point_fail_when_header_missing() {
            // when
            ParameterizedTypeReference<ApiResponse<PointV1Dtos.PointInfoResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PointV1Dtos.PointInfoResponse>> response =
                    testRestTemplate.exchange(Uris.Point.GET_INFO, HttpMethod.GET,
                            new HttpEntity<>(null, null), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }

    @Nested
    @DisplayName("포인트 충전 API")
    class ChargePointTest {

        @Test
        @DisplayName("존재하는 유저가 1000원을 충전하면 충전된 보유 총량을 응답으로 반환한다")
        void charge_1000_returns_total_amount() {
            // given
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            userFacade.registerUser(userCommand);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", userCommand.username());
            headers.setContentType(MediaType.APPLICATION_JSON);

            PointV1Dtos.PointChargeRequest chargeRequest = new PointV1Dtos.PointChargeRequest(new BigDecimal("1000"));

            // when
            ParameterizedTypeReference<ApiResponse<PointV1Dtos.PointChargeResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PointV1Dtos.PointChargeResponse>> response =
                    testRestTemplate.exchange(Uris.Point.CHARGE, HttpMethod.POST,
                            new HttpEntity<>(chargeRequest, headers), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().username())
                            .isEqualTo(userCommand.username()),
                    () -> assertThat(Objects.requireNonNull(response.getBody()).data().totalAmount())
                            .isEqualByComparingTo(new BigDecimal("1000.00"))
            );
        }

        @Test
        @DisplayName("존재하는 유저가 여러 번 충전하면 누적된 총 금액을 응답으로 반환한다")
        void charge_multiple_times_returns_accumulated_amount() {
            // given
            UserRegisterCommand userCommand = UserTestFixture.createDefaultUserCommand();
            userFacade.registerUser(userCommand);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", userCommand.username());
            headers.setContentType(MediaType.APPLICATION_JSON);

            // 1차 충전: 1000원
            PointV1Dtos.PointChargeRequest firstChargeRequest = new PointV1Dtos.PointChargeRequest(new BigDecimal("1000"));

            ParameterizedTypeReference<ApiResponse<PointV1Dtos.PointChargeResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };

            ResponseEntity<ApiResponse<PointV1Dtos.PointChargeResponse>> firstResponse =
                    testRestTemplate.exchange(Uris.Point.CHARGE, HttpMethod.POST,
                            new HttpEntity<>(firstChargeRequest, headers), responseType);

            // 1차 충전 검증
            assertAll(
                    () -> assertTrue(firstResponse.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(firstResponse.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(firstResponse.getBody()).data().totalAmount())
                            .isEqualByComparingTo(new BigDecimal("1000.00"))
            );

            // 2차 충전: 2500원
            PointV1Dtos.PointChargeRequest secondChargeRequest = new PointV1Dtos.PointChargeRequest(new BigDecimal("2500"));

            ResponseEntity<ApiResponse<PointV1Dtos.PointChargeResponse>> secondResponse =
                    testRestTemplate.exchange(Uris.Point.CHARGE, HttpMethod.POST,
                            new HttpEntity<>(secondChargeRequest, headers), responseType);

            // 2차 충전 검증 (1000 + 2500 = 3500)
            assertAll(
                    () -> assertTrue(secondResponse.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(secondResponse.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(secondResponse.getBody()).data().username())
                            .isEqualTo(userCommand.username()),
                    () -> assertThat(Objects.requireNonNull(secondResponse.getBody()).data().totalAmount())
                            .isEqualByComparingTo(new BigDecimal("3500.00"))
            );

            // 3차 충전: 500원
            PointV1Dtos.PointChargeRequest thirdChargeRequest = new PointV1Dtos.PointChargeRequest(new BigDecimal("500"));

            ResponseEntity<ApiResponse<PointV1Dtos.PointChargeResponse>> thirdResponse =
                    testRestTemplate.exchange(Uris.Point.CHARGE, HttpMethod.POST,
                            new HttpEntity<>(thirdChargeRequest, headers), responseType);

            // 3차 충전 검증 (1000 + 2500 + 500 = 4000)
            assertAll(
                    () -> assertTrue(thirdResponse.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(thirdResponse.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(thirdResponse.getBody()).data().username())
                            .isEqualTo(userCommand.username()),
                    () -> assertThat(Objects.requireNonNull(thirdResponse.getBody()).data().totalAmount())
                            .isEqualByComparingTo(new BigDecimal("4000.00"))
            );

            // 최종 포인트 조회로 검증
            ParameterizedTypeReference<ApiResponse<PointV1Dtos.PointInfoResponse>> pointInfoResponseType =
                    new ParameterizedTypeReference<>() {
                    };
            HttpHeaders getHeaders = new HttpHeaders();
            getHeaders.set("X-USER-ID", userCommand.username());

            ResponseEntity<ApiResponse<PointV1Dtos.PointInfoResponse>> pointInfoResponse =
                    testRestTemplate.exchange(Uris.Point.GET_INFO, HttpMethod.GET,
                            new HttpEntity<>(null, getHeaders), pointInfoResponseType);

            // 최종 포인트 조회 검증
            assertAll(
                    () -> assertTrue(pointInfoResponse.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(pointInfoResponse.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(Objects.requireNonNull(pointInfoResponse.getBody()).data().username())
                            .isEqualTo(userCommand.username()),
                    () -> assertThat(Objects.requireNonNull(pointInfoResponse.getBody()).data().currentPointAmount())
                            .isEqualByComparingTo(new BigDecimal("4000.00"))
            );
        }

        @Test
        @DisplayName("존재하지 않는 유저로 요청하면 404 Not Found 응답을 반환한다")
        void charge_with_nonexistent_user_returns_404() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "nonexistentuser");
            headers.setContentType(MediaType.APPLICATION_JSON);

            PointV1Dtos.PointChargeRequest chargeRequest = new PointV1Dtos.PointChargeRequest(new BigDecimal("1000"));

            // when
            ParameterizedTypeReference<ApiResponse<PointV1Dtos.PointChargeResponse>> responseType =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PointV1Dtos.PointChargeResponse>> response =
                    testRestTemplate.exchange(Uris.Point.CHARGE, HttpMethod.POST,
                            new HttpEntity<>(chargeRequest, headers), responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }
}
