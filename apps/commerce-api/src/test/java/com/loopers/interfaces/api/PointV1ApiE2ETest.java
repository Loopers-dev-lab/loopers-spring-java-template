package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.point.PointV1Dto.PointChargeRequest;
import com.loopers.interfaces.api.point.PointV1Dto.PointChargeResponse;
import com.loopers.interfaces.api.point.PointV1Dto.PointResponse;
import com.loopers.utils.DatabaseCleanUp;
import java.time.LocalDate;
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
    private static final String ENDPOINT_GET = "/api/v1/points";
    private static final String ENDPOINT_CHARGE = "/api/v1/points/charge";
    private static final long INITIAL_POINT = 0L;

    private final TestRestTemplate testRestTemplate;
    private final UserJpaRepository userJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public PointV1ApiE2ETest(
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

    @DisplayName("GET /api/v1/points")
    @Nested
    class Get {
        @DisplayName("X-USER-ID 헤더에 존재하는 user ID를 주면 처음 생성한 user의 포인트는 0이고 보유 포인트를 반환한다.")
        @Test
        void returnsPoint_whenHeaderValidUserIdIsProvided() {
            // arrange
            String userId = "test";
            String email = "test@test.com";
            LocalDate birthday = LocalDate.of(2020, 1, 1);
            UserModel saveUserModel = userJpaRepository.save(
                    UserModel.create(userId, email, birthday, Gender.MALE)
            );

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", String.valueOf(saveUserModel.getId()));

            // act
            ParameterizedTypeReference<ApiResponse<PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<PointResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_GET, HttpMethod.GET, new HttpEntity<>(headers), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().userModelId()).isEqualTo(saveUserModel.getId()),
                    () -> assertThat(response.getBody().data().point()).isEqualTo(INITIAL_POINT)
            );
        }

        @DisplayName("X-USER-ID 헤더가 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returns400BadRequest_whenXUserIdHeaderIsMissing() {
            // arrange
            String userId = "test";
            String email = "test@test.com";
            LocalDate birthday = LocalDate.of(2020, 1, 1);
            UserModel saveUserModel = userJpaRepository.save(
                    UserModel.create(userId, email, birthday, Gender.MALE)
            );

            // act
            ParameterizedTypeReference<ApiResponse<PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<PointResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_GET, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @DisplayName("POST /api/v1/points/charge")
    @Nested
    class Charge {
        @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
        @Test
        void returnsTotalPoint_whenValidUserChargesPoint() {
            // arrange
            String userId = "test";
            String email = "test@test.com";
            LocalDate birthday = LocalDate.of(2020, 1, 1);
            UserModel saveUserModel = userJpaRepository.save(
                    UserModel.create(userId, email, birthday, Gender.MALE)
            );
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", String.valueOf(saveUserModel.getId()));
            PointChargeRequest request = new PointChargeRequest(1000L);

            // act
            ParameterizedTypeReference<ApiResponse<PointChargeResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<PointChargeResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_CHARGE, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

            // assert
            assertThat(response.getBody().data().point()).isEqualTo(INITIAL_POINT + request.point());
        }

        @DisplayName("존재하지 않는 유저로 요청할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void returns404NotFound_whenUserDoesNotExist() {
            // arrange
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", String.valueOf(-1L));
            PointChargeRequest request = new PointChargeRequest(1000L);

            // act
            ParameterizedTypeReference<ApiResponse<PointChargeResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<PointChargeResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_CHARGE, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
