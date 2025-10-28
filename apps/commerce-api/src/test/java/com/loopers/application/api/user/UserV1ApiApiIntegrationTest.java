package com.loopers.application.api.user;

import com.loopers.application.api.ApiIntegrationTest;
import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserIdentifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class UserV1ApiApiIntegrationTest extends ApiIntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

    @Autowired
    private UserRepository userRepository;

    @Nested
    @DisplayName("회원가입")
    class 회원가입 {

        @Nested
        @DisplayName("정상 요청인 경우")
        class 정상_요청인_경우 {

            @Test
            @DisplayName("생성된 유저의 정보를 반환한다.")
            void 생성된_유저의_정보를_반환한다() {
                // given
                UserV1Dto.JoinUserRequest request = new UserV1Dto.JoinUserRequest(
                        "user123",
                        "user@example.com",
                        "2000-01-15",
                        "MALE"
                );

                String endPoint = "/api/v1/users/join";
                ParameterizedTypeReference<ApiResponse<UserV1Dto.JoinUserResponse>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                // when
                ResponseEntity<ApiResponse<UserV1Dto.JoinUserResponse>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.POST, new HttpEntity<>(request), responseType);

                // then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertThat(response.getBody()).isNotNull();
                assertThat(response.getBody().data()).isNotNull();
                assertThat(response.getBody().data().identifier()).isEqualTo("user123");
                assertThat(response.getBody().data().email()).isEqualTo("user@example.com");
                assertThat(response.getBody().data().birthday()).isEqualTo("2000-01-15");
                assertThat(response.getBody().data().gender()).isEqualTo("MALE");
            }
        }

        @Nested
        @DisplayName("성별이 없는 경우")
        class 성별이_없는_경우 {

            @Test
            @DisplayName("400 Bad Request 응답을 반환한다.")
            void badRequest응답을_반환한다() {
                // given
                UserV1Dto.JoinUserRequest request = new UserV1Dto.JoinUserRequest(
                        "user123",
                        "user@example.com",
                        "2000-01-15",
                        null
                );

                String endPoint = "/api/v1/users/join";
                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.POST, new HttpEntity<>(request), responseType);

                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            }
        }
    }

    @Nested
    @DisplayName("내 정보 조회")
    class 내_정보_조회 {

        @Nested
        @DisplayName("성공할 경우")
        class 성공할_경우 {

            @BeforeEach
            void setUp() {
                userRepository.save(
                        User.create(
                                UserIdentifier.create("kilian"),
                                UserEmail.create("kilian@gmail.com"),
                                UserBirthDay.create("1997-10-08"),
                                UserGender.create("MALE")
                        )
                );
            }

            @Test
            @DisplayName("해당하는 유저 정보를 응답으로 반환한다.")
            void 해당하는_유저_정보를_응답으로_반환한다() {
                // given
                String identifier = "kilian";
                String endPoint = "/api/v1/users/" + identifier;
                ParameterizedTypeReference<ApiResponse<UserV1Dto.GetUserResponse>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                // when
                ResponseEntity<ApiResponse<UserV1Dto.GetUserResponse>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.GET, HttpEntity.EMPTY, responseType);

                // then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
                assertSoftly(softly -> {
                    softly.assertThat(response.getBody()).isNotNull();
                    softly.assertThat(response.getBody().data()).isNotNull();
                    softly.assertThat(response.getBody().data().identifier()).isEqualTo("kilian");
                    softly.assertThat(response.getBody().data().email()).isEqualTo("kilian@gmail.com");
                    softly.assertThat(response.getBody().data().birthday()).isEqualTo("1997-10-08");
                    softly.assertThat(response.getBody().data().gender()).isEqualTo("MALE");
                });
            }
        }

        @Nested
        @DisplayName("존재하지 않는 ID로 조회할 경우")
        class 존재하지_않는_ID로_조회할_경우 {

            @Test
            @DisplayName("404 Not Found 응답을 반환한다.")
            void notFound응답을_반환한다() {
                // given
                String identifier = "nonexistent";
                String endPoint = "/api/v1/users/" + identifier;
                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                // when
                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.GET, HttpEntity.EMPTY, responseType);

                // then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            }
        }
    }
}
