package com.loopers.interfaces.api.user;

import com.loopers.domain.user.Gender;
import com.loopers.domain.user.UserModel;
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
import org.springframework.http.*;

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest {
    private static final String ENDPOINT = "/api/v1/users";
    private static final Function<String, String> ENDPOINT_GET = id -> ENDPOINT + "/" + id;

    private static final String USER_ID = "abc123";
    private static final String EMAIL = "abc@sample.com";
    private static final String BIRTH_DATE = "2000-01-01";

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

    @DisplayName("POST /api/v1/users")
    @Nested
    class SignUp {

        @Test
        void 회원_가입이_성공할_경우_생성된_유저_정보를_응답으로_반환한다() {
            // arrange
            UserModel user = userJpaRepository.save(UserModel.create(USER_ID, EMAIL, BIRTH_DATE, Gender.FEMALE));

            String requestUrl = ENDPOINT_GET.apply(USER_ID);

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType = new ParameterizedTypeReference<>() {
            };
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            // assert
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo(user.getUserId()),
                    () -> assertThat(response.getBody().data().email()).isEqualTo(user.getEmail()),
                    () -> assertThat(response.getBody().data().birthDate()).isEqualTo(user.getBirthDate())
            );
        }

        @Test
        void 회원_가입_시에_성별이_없을_경우_400_Bad_Request_응답을_반환한다() {
            // arrange
            UserV1Dto.UserCreateRequest request = new UserV1Dto.UserCreateRequest(
                    USER_ID, EMAIL, BIRTH_DATE, null
            );

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<UserV1Dto.UserCreateRequest> entity = new HttpEntity<>(request, headers);

            // act
            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, entity,
                            new ParameterizedTypeReference<>() {
                            });

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST),
                    () -> assertThat(userJpaRepository.findAll()).isEmpty()
            );
        }
    }


    @DisplayName("GET /api/v1/users/{id}")
    @Nested
    class Get {

        @Test
        void 내_정보_조회에_성공할_경우_해당하는_유저_정보를_응답으로_반환한다() {
            // arrange
            UserModel user = userJpaRepository.save(
                    UserModel.create(USER_ID, EMAIL, BIRTH_DATE, Gender.FEMALE)
            );

            String requestUrl = ENDPOINT_GET.apply(USER_ID);

            // act
            ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>> responseType =
                    new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.GET, HttpEntity.EMPTY, responseType);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody()).isNotNull(),
                    () -> assertThat(response.getBody().data()).isNotNull(),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo(user.getUserId()),
                    () -> assertThat(response.getBody().data().email()).isEqualTo(user.getEmail()),
                    () -> assertThat(response.getBody().data().birthDate()).isEqualTo(user.getBirthDate()),
                    () -> assertThat(response.getBody().data().gender()).isEqualTo(user.getGender())
            );
        }


        @Test
        void 존재하지_않는_ID로_조회할_경우_404_Not_Found_응답을_반환한다() {
            // arrange
            String requestUrl = ENDPOINT_GET.apply(USER_ID);

            // act
            ParameterizedTypeReference<ApiResponse<Object>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<Object>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.GET, HttpEntity.EMPTY, responseType);

            // assert
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND),
                    () -> assertThat(response.getBody()).isNotNull()
            );
        }
    }
}
