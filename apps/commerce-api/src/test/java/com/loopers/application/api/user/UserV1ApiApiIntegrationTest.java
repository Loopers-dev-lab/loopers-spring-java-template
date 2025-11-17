package com.loopers.application.api.user;

import com.loopers.application.api.ApiIntegrationTest;
import com.loopers.application.api.common.dto.ApiResponse;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.user.JoinUserService;
import com.loopers.core.service.user.command.JoinUserCommand;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

class UserV1ApiApiIntegrationTest extends ApiIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JoinUserService joinUserService;

    @Nested
    @DisplayName("회원가입")
    class 회원가입 {

        @Nested
        @DisplayName("정상 요청인 경우")
        class 정상_요청인_경우 {

            @Test
            @DisplayName("Status 200")
            void status200() {
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
            }
        }

        @Nested
        @DisplayName("성별이 없는 경우")
        class 성별이_없는_경우 {

            @Test
            @DisplayName("Status 400")
            void status400() {
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
                        Instancio.of(User.class)
                                .set(field(User::getId), UserId.empty())
                                .set(field(User::getIdentifier), new UserIdentifier("kilian"))
                                .set(field(User::getEmail), new UserEmail("kilian@gmail.com"))
                                .create()
                );
            }

            @Test
            @DisplayName("Status 200")
            void status200() {
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
            }
        }

        @Nested
        @DisplayName("존재하지 않는 ID로 조회할 경우")
        class 존재하지_않는_ID로_조회할_경우 {

            @Test
            @DisplayName("Status 404")
            void status404() {
                // given
                String identifier = "nonExist";
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

    @Nested
    @DisplayName("포인트 조회")
    class 포인트_조회 {

        @Nested
        @DisplayName("성공할 경우")
        class 성공할_경우 {

            @BeforeEach
            void setUp() {
                joinUserService.joinUser(new JoinUserCommand(
                        "kilian",
                        "kilian@gmail.com",
                        "1997-10-08",
                        "MALE"
                ));
            }

            @Test
            @DisplayName("Status 200")
            void status200() {
                // given
                String userIdentifier = "kilian";
                String endPoint = "/api/v1/users/points";
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("X-USER-ID", userIdentifier);
                HttpEntity<Void> httpEntity = new HttpEntity<>(headers);
                ParameterizedTypeReference<ApiResponse<UserV1Dto.GetUserPointResponse>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                // when
                ResponseEntity<ApiResponse<UserV1Dto.GetUserPointResponse>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.GET, httpEntity, responseType);

                // then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }
        }

        @Nested
        @DisplayName("X-USER-ID 헤더가 없을 경우")
        class X_USER_ID_헤더가_없을_경우 {

            @Test
            @DisplayName("Status 400")
            void status400() {
                // given
                String endPoint = "/api/v1/users/points";
                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                // when
                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.GET, HttpEntity.EMPTY, responseType);

                // then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            }
        }
    }

    @Nested
    @DisplayName("포인트 충전")
    class 포인트_충전 {

        @Nested
        @DisplayName("존재하는 유저가 1000원을 충전할 경우")
        class 존재하는_유저가_충전 {

            @BeforeEach
            void setUp() {
                joinUserService.joinUser(new JoinUserCommand(
                        "kilian",
                        "kilian@gmail.com",
                        "1997-10-08",
                        "MALE"
                ));
            }

            @Test
            @DisplayName("Status 200")
            void status200() {
                // given
                String userIdentifier = "kilian";
                String endPoint = "/api/v1/users/points/charge";
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("X-USER-ID", userIdentifier);
                UserV1Dto.UserPointChargeRequest request = new UserV1Dto.UserPointChargeRequest(new BigDecimal(1000));
                HttpEntity<UserV1Dto.UserPointChargeRequest> httpEntity = new HttpEntity<>(request, headers);
                ParameterizedTypeReference<ApiResponse<UserV1Dto.UserPointChargeResponse>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                // when
                ResponseEntity<ApiResponse<UserV1Dto.UserPointChargeResponse>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.POST, httpEntity, responseType);

                // then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            }
        }

        @Nested
        @DisplayName("존재하지 않는 유저로 요청할 경우")
        class 존재하지_않는_유저로_요청 {

            @Test
            @DisplayName("Status 404")
            void status404() {
                // given
                String userIdentifier = "nonExist";
                String endPoint = "/api/v1/users/points/charge";
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("X-USER-ID", userIdentifier);
                UserV1Dto.UserPointChargeRequest request = new UserV1Dto.UserPointChargeRequest(new BigDecimal(1000));
                HttpEntity<UserV1Dto.UserPointChargeRequest> httpEntity = new HttpEntity<>(request, headers);
                ParameterizedTypeReference<ApiResponse<Void>> responseType =
                        new ParameterizedTypeReference<>() {
                        };

                // when
                ResponseEntity<ApiResponse<Void>> response =
                        testRestTemplate.exchange(endPoint, HttpMethod.POST, httpEntity, responseType);

                // then
                assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            }
        }
    }
}
