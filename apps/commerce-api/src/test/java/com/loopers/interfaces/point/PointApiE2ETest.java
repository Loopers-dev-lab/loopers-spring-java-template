package com.loopers.interfaces.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.utils.DatabaseCleanUp;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.ApiResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PointApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final UserJpaRepository userJpaRepository;
    private final PointJpaRepository pointJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    private static final Function<String, String> ENDPOINT_GET =
        userId -> "/api/v1/points/" + userId;

    @Autowired
    public PointApiE2ETest(
        TestRestTemplate testRestTemplate,
        UserJpaRepository userJpaRepository,
        PointJpaRepository pointJpaRepository,
        DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.userJpaRepository = userJpaRepository;
        this.pointJpaRepository = pointJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/points/{userId}")
    @Nested
    class GetPoint {

        @Test
        @DisplayName("존재하는 포인트 정보를 주면, 해당 포인트 정보를 반환한다")
        void returnsPointInfo_whenPointExists() {
            // arrange
            String userId = "testuser";
            String email = "test@example.com";
            LocalDate birth = LocalDate.of(1990, 1, 1);
            Gender gender = Gender.MALE;

            User user = User.of(userId, email, birth, gender);
            User savedUser = userJpaRepository.save(user);

            Point point = new Point(savedUser);
            pointJpaRepository.save(point);

            String requestUrl = ENDPOINT_GET.apply(userId);

            // act
            ParameterizedTypeReference<ApiResponse<PointDto.Response>> responseType =
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointDto.Response>> response =
                testRestTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(null),
                    responseType
                );

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().userId()).isEqualTo(userId),
                () -> assertThat(response.getBody().data().balance()).isZero()
            );
          }

        @Test
        @DisplayName("존재하지 않는 userId를 주면, null을 반환한다")
        void returnsNull_whenPointDoesNotExist() {
            // arrange
            String nonExistentUserId = "nonexistent";
            String requestUrl = ENDPOINT_GET.apply(nonExistentUserId);

            // act
            ParameterizedTypeReference<ApiResponse<PointDto.Response>> responseType =
                new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointDto.Response>> response =
                testRestTemplate.exchange(
                    requestUrl,
                    HttpMethod.GET,
                    new HttpEntity<>(null),
                    responseType
                );

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data()).isNull()
            );
        }
    }
}
