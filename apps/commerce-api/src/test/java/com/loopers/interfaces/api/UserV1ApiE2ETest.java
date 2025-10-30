package com.loopers.interfaces.api;

import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.user.UserV1Dto;
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
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest {

    private static final Function<Long, String> ENDPOINT_GET = id -> "/api/v1/examples/" + id;
    private static final String ENDPOINT_SIGNUP = "/api/v1/user";

    private final TestRestTemplate testRestTemplate;
    private final UserJpaRepository userJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;


    @Autowired
    public UserV1ApiE2ETest(
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

    /*
    - [x]  회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.
    - [x]  회원 가입 시에 성별이 없을 경우, `400 Bad Request` 응답을 반환한다.
     */

    @DisplayName("GET /api/v1/examples/{id}")
    @Nested
    class Get {
        /*@DisplayName("존재하는 예시 ID를 주면, 해당 예시 정보를 반환한다.")
        @Test
        void returnsExampleInfo_whenValidIdIsProvided() {
            // arrange
            UserModel userModel = userJpaRepository.save(
                new UserModel("예시 제목", "예시 설명")
            );
            String requestUrl = ENDPOINT_GET.apply(userModel.getId());

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.ExampleResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.ExampleResponse>> response =
                testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                () -> assertThat(response.getBody().data().id()).isEqualTo(userModel.getId()),
                () -> assertThat(response.getBody().data().name()).isEqualTo(userModel.getName()),
                () -> assertThat(response.getBody().data().description()).isEqualTo(userModel.getDescription())
            );
        }

        @DisplayName("숫자가 아닌 ID 로 요청하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        void throwsBadRequest_whenIdIsNotProvided() {
            // arrange
            String requestUrl = "/api/v1/examples/나나";

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.ExampleResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.ExampleResponse>> response =
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
            ParameterizedTypeReference<ApiResponse<UserV1Dto.ExampleResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.ExampleResponse>> response =
                testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                () -> assertTrue(response.getStatusCode().is4xxClientError()),
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }*/
    }

    @DisplayName("POST /api/v1/user")
    @Nested
    class Post {

        @DisplayName("회원가입 성공시, 생성된 유저 정보를 응답으로 반환한다")
        @Test
        void returnUserResponse_whenSuccessful() {
            // given
            UserV1Dto.UserCreateRequest request = new UserV1Dto.UserCreateRequest(
                    "ajchoi0928",
                    "junho",
                    "loopers backend developer",
                    "loopers@loopers.com",
                    "1997-09-28",
                    "M"
            );

            // var json = objectMapper.writeValueAsString(request);

            // when
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_SIGNUP,
                            HttpMethod.POST,
                            json(request),
                            new ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {
                            }
                    );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result())
                    .isEqualTo(ApiResponse.Metadata.Result.SUCCESS);

            var data = response.getBody().data();
            assertThat(data).isNotNull();
            assertThat(data.userId()).isEqualTo("ajchoi0928");
            assertThat(data.userName()).isEqualTo("junho");
            assertThat(data.email()).isEqualTo("loopers@loopers.com");

            // DB 검증(선택)
            assertThat(userJpaRepository.findByUserId("ajchoi0928")).isPresent();
        }

        @DisplayName("회원가입 시 성별 정보가 없는 경우 '400 Bad Reqeuset' 응답을 반환한다")
        @Test
        void throwsException_whenBlankGenderIsProvided() {
            // given
            UserV1Dto.UserCreateRequest request = new UserV1Dto.UserCreateRequest(
                    "ajchoi0928",
                    "junho",
                    "loopers backend developer",
                    "loopers@loopers.com",
                    "1997-09-28",
                    ""
            );

            // when
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_SIGNUP,
                            HttpMethod.POST,
                            json(request),
                            new ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {
                            }
                    );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result())
                    .isEqualTo(ApiResponse.Metadata.Result.FAIL);
            assertThat(response.getBody().meta().errorCode()).isNotBlank();
            assertThat(response.getBody().meta().message()).isNotBlank();


        }


        private static HttpEntity<Object> json(Object body) {
            HttpHeaders h = new HttpHeaders();
            h.setContentType(MediaType.APPLICATION_JSON);
            return new HttpEntity<>(body, h);
        }

    }
}
