package com.loopers.interfaces.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.user.UserV1Dto;
import com.loopers.interfaces.api.user.UserV1Dto.UserSignUpRequest;
import com.loopers.interfaces.api.user.UserV1Dto.UserSignUpResponse;
import com.loopers.utils.DatabaseCleanUp;
import java.time.LocalDate;
import java.util.function.Function;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest {
    private static final Function<UserSignUpRequest, String> ENDPOINT_POST = request -> "/api/v1/users";
    private static final Function<Long, String> ENDPOINT_GET = id -> "/api/v1/users/" + id;

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

    @DisplayName("Post /api/v1/users")
    @Nested
    class Post {
        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        @Test
        void returnsUserSignUpResponse_whenValidUserSignUpRequest() {
            // arrange
            String userId = "test";
            String email = "test@test.com";
            String birthday = "2020-01-01";
            UserSignUpRequest request = new UserSignUpRequest(userId, email, birthday, Gender.MALE);

            String requestUrl = ENDPOINT_POST.apply(request);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<UserSignUpResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserSignUpResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().id()).isNotNull(),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo(request.userId()),
                    () -> assertThat(response.getBody().data().email()).isEqualTo(request.email()),
                    () -> assertThat(response.getBody().data().birthday()).isEqualTo(request.birthday())
            );
        }

        @DisplayName("회원 가입 시에 성별이 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void returns400BadRequest_whenUserSignUpGenderIsNull() {
            // arrange
            String userId = "test";
            String email = "test@test.com";
            String birthday = "2020-01-01";
            UserSignUpRequest request = new UserSignUpRequest(userId, email, birthday, null);
            String requestUrl = ENDPOINT_POST.apply(request);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            // act
            ParameterizedTypeReference<ApiResponse<UserSignUpResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserSignUpResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(request, headers), responseType);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @DisplayName("GET /api/v1/users/{id}")
    @Nested
    class Get {
        @DisplayName("존재하는 user ID를 주면, 해당 user 정보를 반환한다.")
        @Test
        void returnsUserResponse_whenValidIdIsProvided() {
            // arrange
            String userId = "test";
            String email = "test@test.com";
            LocalDate birthday = LocalDate.of(2020, 1, 1);
            User saveUser = userJpaRepository.save(
                    User.create(userId, email, birthday, Gender.MALE)
            );
            String requestUrl = ENDPOINT_GET.apply(saveUser.getId());

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().id()).isEqualTo(saveUser.getId()),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo(saveUser.getUserId())
            );
        }

        @DisplayName("존재하지 않는 user ID를 주면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        void throwsException_whenInvalidIdIsProvided() {
            // arrange
            Long invalidId = -1L;
            String requestUrl = ENDPOINT_GET.apply(invalidId);

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }
}
