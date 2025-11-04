package com.loopers.interfaces.api.point;

import static org.assertj.core.api.Assertions.*;

import com.loopers.application.user.UserCommand.SignupCommand;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserService;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PointV1ApiE2ETest {

    private static final String ENDPOINT_CHARGE_POINT = "/api/v1/point";

    private final TestRestTemplate testRestTemplate;
    private final UserService userService;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public PointV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            UserService userService,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.userService = userService;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("POST /api/v1/point - 포인트 충전")
    @Nested
    class ChargePoint {

        @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
        @Test
        void returnsTotalPoint_whenUserExistsAndCharges1000() {
            // arrange
            SignupCommand signupCommand = new SignupCommand(
                    "testId123",
                    "test@test.com",
                    "2000-03-29",
                    "F"
            );
            User user = userService.signup(signupCommand);

            PointV1Dto.PointRequest request = new PointV1Dto.PointRequest(1000);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId().getId());

            // act
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(
                    ENDPOINT_CHARGE_POINT,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);

            PointV1Dto.PointResponse pointResponse = response.getBody().data();
            assertThat(pointResponse.totalPoint()).isEqualTo(1000);
        }

        @DisplayName("존재하지 않는 유저로 요청할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void returns404_whenUserDoesNotExist() {
            // arrange
            String nonExistentLoginId = "nonExistentUser";
            PointV1Dto.PointRequest request = new PointV1Dto.PointRequest(1000);

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", nonExistentLoginId);

            // act
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(
                    ENDPOINT_CHARGE_POINT,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.FAIL);
            assertThat(response.getBody().meta().message()).contains("사용자를 찾을 수 없습니다");
        }

        @DisplayName("기존 포인트가 있는 유저가 추가 충전할 경우, 누적된 총량을 응답으로 반환한다.")
        @Test
        void returnsAccumulatedTotalPoint_whenUserAlreadyHasPoint() {
            // arrange
            SignupCommand signupCommand = new SignupCommand(
                    "testId123",
                    "test@test.com",
                    "2000-03-29",
                    "F"
            );
            User user = userService.signup(signupCommand);

            PointV1Dto.PointRequest firstRequest = new PointV1Dto.PointRequest(1000);
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId().getId());

            testRestTemplate.exchange(
                    ENDPOINT_CHARGE_POINT,
                    HttpMethod.POST,
                    new HttpEntity<>(firstRequest, headers),
                    new ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>>() {}
            );

            PointV1Dto.PointRequest secondRequest = new PointV1Dto.PointRequest(500);

            // act
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(
                    ENDPOINT_CHARGE_POINT,
                    HttpMethod.POST,
                    new HttpEntity<>(secondRequest, headers),
                    new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);

            PointV1Dto.PointResponse pointResponse = response.getBody().data();
            assertThat(pointResponse.totalPoint()).isEqualTo(1500);
        }
    }

    @DisplayName("GET /api/v1/point - 포인트 조회")
    @Nested
    class GetPointInfo {

        @DisplayName("포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.")
        @Test
        void returnsPointInfo_whenUserExists() {
            // arrange
            SignupCommand signupCommand = new SignupCommand(
                    "testId123",
                    "test@test.com",
                    "2000-03-29",
                    "F"
            );
            User user = userService.signup(signupCommand);

            PointV1Dto.PointRequest chargeRequest = new PointV1Dto.PointRequest(1200);
            HttpHeaders chargeHeaders = new HttpHeaders();
            chargeHeaders.set("X-USER-ID", user.getLoginId().getId());

            testRestTemplate.exchange(
                    ENDPOINT_CHARGE_POINT,
                    HttpMethod.POST,
                    new HttpEntity<>(chargeRequest, chargeHeaders),
                    new ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>>() {}
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getLoginId().getId());

            // act
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response = testRestTemplate.exchange(
                    ENDPOINT_CHARGE_POINT,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    new ParameterizedTypeReference<>() {}
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result()).isEqualTo(ApiResponse.Metadata.Result.SUCCESS);

            PointV1Dto.PointResponse pointResponse = response.getBody().data();
            assertThat(pointResponse.totalPoint()).isEqualTo(1200);
        }

        @DisplayName("X-USER-ID 헤더가 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returns400_whenXUserIdHeaderIsMissing() {
            // arrange
            HttpHeaders headers = new HttpHeaders();

            // act
            ResponseEntity<String> response = testRestTemplate.exchange(
                    ENDPOINT_CHARGE_POINT,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    String.class
            );

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
