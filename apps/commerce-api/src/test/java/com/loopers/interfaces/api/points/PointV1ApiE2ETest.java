package com.loopers.interfaces.api.points;

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
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PointV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final MemberFacade memberFacade;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public PointV1ApiE2ETest(
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

    @DisplayName("포인트 조회 (GET /api/v1/points)")
    @Nested
    class PointInquiry {
        
        @DisplayName("유효한 사용자 ID로 조회 시 200과 보유 포인트를 반환한다")
        @Test
        void shouldReturn200AndPoints_whenValidUserIdProvided() {
            memberFacade.registerMember("test123", "홍길동", "test@example.com", "password", "1990-01-01", "MALE");

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", "test123");

            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = 
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
                    testRestTemplate.exchange("/api/v1/points", HttpMethod.GET, 
                            new HttpEntity<>(null, headers), responseType);

            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().points()).isEqualTo(BigDecimal.ZERO)
            );
        }

        @DisplayName("X-USER-ID 헤더가 누락된 경우 400 Bad Request를 반환한다")
        @Test
        void shouldReturn400_whenUserIdHeaderIsMissing() {
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = 
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
                    testRestTemplate.exchange("/api/v1/points", HttpMethod.GET, 
                            new HttpEntity<>(null), responseType);

            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }
    }
}