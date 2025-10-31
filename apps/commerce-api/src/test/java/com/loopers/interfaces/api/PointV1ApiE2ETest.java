package com.loopers.interfaces.api;

import com.loopers.domain.point.Point;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.interfaces.api.point.PointV1Dto;
import com.loopers.testcontainers.MySqlTestContainersConfig;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Import;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(MySqlTestContainersConfig.class)
class PointV1ApiE2ETest {

    private static final String ENDPOINT_GET_POINT = "/api/v1/point";

    private final TestRestTemplate testRestTemplate;
    private final PointJpaRepository pointJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public PointV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            PointJpaRepository pointJpaRepository,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.pointJpaRepository = pointJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/point")
    @Nested
    class GetPoint {

        @DisplayName("포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.")
        @Test
        void returnsPointAmount_whenPointExists() {
            // given
            String userId = "testuser1";
            Long amount = 1000L;

            // 포인트 생성
            pointJpaRepository.save(Point.create(userId, amount));

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", userId);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // when
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_GET_POINT, HttpMethod.GET, requestEntity, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().amount()).isEqualTo(amount)
            );
        }

        @DisplayName("포인트가 없는 경우에도 0을 응답으로 반환한다.")
        @Test
        void returnsZero_whenPointDoesNotExist() {
            // given
            String userId = "testuser1";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", userId);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // when
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_GET_POINT, HttpMethod.GET, requestEntity, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data().amount()).isEqualTo(0L)
            );
        }

        @DisplayName("X-USER-ID 헤더가 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenUserIdHeaderIsMissing() {
            // given
            HttpEntity<Void> requestEntity = new HttpEntity<>(null);

            // when
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_GET_POINT, HttpMethod.GET, requestEntity, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @DisplayName("X-USER-ID 헤더가 빈 문자열일 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returnsBadRequest_whenUserIdHeaderIsEmpty() {
            // given
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "");
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // when
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_GET_POINT, HttpMethod.GET, requestEntity, responseType);

            // then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }
}
