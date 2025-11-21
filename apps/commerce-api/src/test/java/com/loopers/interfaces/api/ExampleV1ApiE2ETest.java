package com.loopers.interfaces.api;

import com.loopers.domain.example.ExampleModel;
import com.loopers.infrastructure.example.ExampleJpaRepository;
import com.loopers.interfaces.api.example.ExampleV1Dto;
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
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ExampleV1ApiE2ETest {

    private static final Function<Long, String> ENDPOINT_GET = id -> "/api/v1/examples/" + id;

    private final TestRestTemplate testRestTemplate;
    private final ExampleJpaRepository exampleJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public ExampleV1ApiE2ETest(
        TestRestTemplate testRestTemplate,
        ExampleJpaRepository exampleJpaRepository,
        DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.exampleJpaRepository = exampleJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/examples/{id}")
    @Nested
    class Get {
        @DisplayName("존재하는 예시 ID를 주면, 해당 예시 정보를 반환한다.")
        @Test
        void returnsExampleInfo_whenValidIdIsProvided() {
            // arrange
            ExampleModel exampleModel = exampleJpaRepository.save(
                new ExampleModel("예시 제목", "예시 설명")
            );
            String requestUrl = ENDPOINT_GET.apply(exampleModel.getId());

            // act
            ParameterizedTypeReference<ApiResponse<ExampleV1Dto.ExampleResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ExampleV1Dto.ExampleResponse>> response =
                testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().id()).isEqualTo(exampleModel.getId()),
                () -> assertThat(response.getBody().data().name()).isEqualTo(exampleModel.getName()),
                () -> assertThat(response.getBody().data().description()).isEqualTo(exampleModel.getDescription())
            );
        }

        @DisplayName("숫자가 아닌 ID 로 요청하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        void throwsBadRequest_whenIdIsNotProvided() {
            // arrange
            String requestUrl = "/api/v1/examples/나나";

            // act
            ParameterizedTypeReference<ApiResponse<ExampleV1Dto.ExampleResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ExampleV1Dto.ExampleResponse>> response =
                testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @DisplayName("존재하지 않는 예시 ID를 주면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        void throwsException_whenInvalidIdIsProvided() {
            // arrange
            Long invalidId = -1L;
            String requestUrl = ENDPOINT_GET.apply(invalidId);

            // act
            ParameterizedTypeReference<ApiResponse<ExampleV1Dto.ExampleResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<ExampleV1Dto.ExampleResponse>> response =
                testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }
}
