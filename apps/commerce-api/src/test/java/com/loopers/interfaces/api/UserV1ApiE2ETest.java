package com.loopers.interfaces.api;

import com.loopers.domain.user.UserModel;
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

import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest {

    private static final Function<String, String> ENDPOINT_GETUSER = id -> "/api/v1/user/" + id;
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
    # E2E_회원 가입
    - [x]  회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.
    - [x]  회원 가입 시에 성별이 없을 경우, `400 Bad Request` 응답을 반환한다.
    # E2E_내 정보 조회
    - [x]  내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.
    - [x]  존재하지 않는 ID 로 조회할 경우, `404 Not Found` 응답을 반환한다.

     */

    @DisplayName("GET /api/v1/user/{id}")
    @Nested
    class Get {
        @DisplayName("내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다")
        @Test
        void returnsUserInfo_whenValidUserIdIsProvided() {
            // given
            String userId = "ajchoi0928";
            String userName = "junho";
            String description = "loopers backend developer";
            String email = "loopers@loopers.com";
            String birthDate = "1997-09-28";
            String gender = "M";
            UserModel userModel = new UserModel(userId, userName, description, email, birthDate, gender);
            userJpaRepository.save(userModel);

            //when
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_GETUSER.apply(userId),
                            HttpMethod.GET,
                            new HttpEntity<>(null),
                            new ParameterizedTypeReference<>() {
                            }
                    );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result())
                    .isEqualTo(ApiResponse.Metadata.Result.SUCCESS);

            var data = response.getBody().data();
            assertThatSameValue(data, userModel);
        }

        @DisplayName("존재하지 않는 ID 로 조회할 경우, `404 Not Found` 응답을 반환한다")
        @Test
        void throwsNotFoundException_whenInvalidUserIdIsProvided() {
            // given
            String userId = "ajchoi0928";
            // 테스트 긴한데 DELETE 날려도 괜찮을까? DELETE 치는 건 mock 해야하나?
            userJpaRepository.deleteByUserId(userId);

            // when
            ResponseEntity<ApiResponse<UserV1Dto.UserResponse>> response =
                    testRestTemplate.exchange(ENDPOINT_GETUSER.apply(userId),
                            HttpMethod.GET,
                            new HttpEntity<>(null),
                            new ParameterizedTypeReference<>() {
                            }
                    );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            // [WARN] 에러일 때도 @ControllerAdvice가 ApiResponse.fail(...) 바디를 만듦 주의
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result())
                    .isEqualTo(ApiResponse.Metadata.Result.FAIL);
            assertThat(response.getBody().data()).isNull();
            // 에러메시지 내용 검증은 skip

        }
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
                            new ParameterizedTypeReference<>() {
                            }
                    );

            // then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().meta().result())
                    .isEqualTo(ApiResponse.Metadata.Result.SUCCESS);

            var data = response.getBody().data();
            // 응답값과 실제 저장된 엔티티 검증
            var savedUser = userJpaRepository.findByUserId("ajchoi0928").get();
            assertThatSameValue(data, savedUser);

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
                            new ParameterizedTypeReference<>() {
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


    private static void assertThatSameValue (UserV1Dto.UserResponse actual, UserModel entity) {
        assertThat(actual).isNotNull();
        assertThat(actual.userId()).isEqualTo(entity.getUserId());
        assertThat(actual.userName()).isEqualTo(entity.getUserName());
        assertThat(actual.description()).isEqualTo(entity.getDescription());
        assertThat(actual.email()).isEqualTo(entity.getEmail());
        assertThat(actual.birthDate()).isEqualTo(entity.getBirthDate());
        assertThat(actual.gender()).isEqualTo(entity.getGender());
    }
}
