package com.loopers.interfaces.api.point;

import com.loopers.domain.point.PointAccount;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserModel;
import com.loopers.infrastructure.point.PointAccountJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
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
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PointV1ApiE2ETest {
    private static final String ENDPOINT = "/api/v1/points";

    private static final String USER_ID = "abc123";
    private static final String EMAIL = "abc@sample.com";
    private static final String BIRTH_DATE = "2000-01-01";

    private final TestRestTemplate testRestTemplate;
    private final UserJpaRepository userJpaRepository;
    private final PointAccountJpaRepository pointAccountJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public PointV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            UserJpaRepository userJpaRepository,
            PointAccountJpaRepository pointAccountJpaRepository,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.userJpaRepository = userJpaRepository;
        this.pointAccountJpaRepository = pointAccountJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/points")
    @Nested
    class GetBalance {

        @DisplayName("포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.")
        @Test
        void pointTest1() {
            // arrange
            UserModel user = userJpaRepository.save(
                    UserModel.create(USER_ID, EMAIL, BIRTH_DATE, Gender.FEMALE)
            );
            pointAccountJpaRepository.save(PointAccount.create(user.getUserId()));

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-USER-ID", user.getUserId());
            HttpEntity<Void> entity = new HttpEntity<>(headers);


            // act
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointBalanceResponse>> type =
                    new ParameterizedTypeReference<>() {
                    };
            ResponseEntity<ApiResponse<PointV1Dto.PointBalanceResponse>> response =
                    testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, entity, type);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode().is2xxSuccessful()).isTrue(),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo(user.getUserId()),
                    () -> assertThat(response.getBody().data().balance()).isEqualTo(0L)
            );
        }


        @DisplayName("X-USER-ID 헤더가 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void pointTest2() {
            // arrange
            HttpEntity<Void> entity = new HttpEntity<>(null, new HttpHeaders());
            // act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, entity, responseType);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode().is4xxClientError()).isTrue(),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNull(),
                    () -> assertThat(response.getBody().meta().message()).contains("요청 헤더 'X-USER-ID'는 필수입니다.")
            );
        }
    }
}
