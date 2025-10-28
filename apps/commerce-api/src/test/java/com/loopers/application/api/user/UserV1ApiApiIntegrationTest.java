package com.loopers.application.api.user;

import com.loopers.application.api.ApiIntegrationTest;
import com.loopers.application.api.common.dto.ApiResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class UserV1ApiApiIntegrationTest extends ApiIntegrationTest {

    @Autowired
    private TestRestTemplate testRestTemplate;

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
}
