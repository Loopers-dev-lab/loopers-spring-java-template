package com.loopers.interfaces.api;

import com.loopers.domain.point.PointService;
import com.loopers.infrastructure.point.PointJpaRepository;
import com.loopers.infrastructure.user.UserJpaRepository;
import com.loopers.interfaces.api.user.UserRequestDto;
import com.loopers.interfaces.api.user.UserResponseDto;
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
import org.springframework.boot.test.mock.mockito.MockBean;


import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final UserJpaRepository userJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;
    private final PointJpaRepository pointJpaRepository;


    @Autowired
    public UserV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            UserJpaRepository userJpaRepository,
            DatabaseCleanUp databaseCleanUp,
            PointJpaRepository pointJpaRepository
    ) {
        this.testRestTemplate = testRestTemplate;
        this.userJpaRepository = userJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
        this.pointJpaRepository = pointJpaRepository;
    }

    @MockBean
    private PointService pointService;

    @AfterEach
    void tearDown() {databaseCleanUp.truncateAllTables(); }

    @DisplayName("회원 관리 API")
    @Nested
    class SignUp {
        @DisplayName("회원 가입이 성공할 경우, 생성된 유저 정보를 응답으로 반환한다.")
        @Test
        void signUP_success_returns_created_user_info(){
            //given
            UserRequestDto requestDto = new UserRequestDto(
                "sangdon",
                "dori@dori.com",
                "1998-02-21",
                "MALE"
            );
            //when
            ResponseEntity<ApiResponse<UserResponseDto>> response = testRestTemplate.exchange(
                "/api/v1/users",
                HttpMethod.POST,
                new HttpEntity<>(requestDto),
                new ParameterizedTypeReference<ApiResponse<UserResponseDto>>() {}
            );
            //then
            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody().data().id()).isEqualTo("sangdon"),
                    () -> assertThat(response.getBody().data().gender()).isEqualTo("MALE"),
                    () -> assertThat(response.getBody().data().birthDate()).isEqualTo("1998-02-21"),
                    () -> assertThat(response.getBody().data().email()).isEqualTo("dori@dori.com")
            );
        }

        @DisplayName("회원 가입 시에 성별이 없을 경우, 400 Bad Request 응답을 반환한다.")
        @Test
        void throwsBadRequest_whenGenderIsNotProvided(){

            //given
            UserRequestDto requestDto = new UserRequestDto(
                    "sangdon",
                    "dori@dori.com",
                    "1998-02-21",
                    ""
            );

            //when
            ResponseEntity<ApiResponse<UserResponseDto>> response = testRestTemplate.exchange(
                    "/api/v1/users",
                    HttpMethod.POST,
                    new HttpEntity<>(requestDto),
                    new ParameterizedTypeReference<ApiResponse<UserResponseDto>>() {}
            );

            //then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
            );
        }

        @DisplayName("내 정보 조회에 성공할 경우, 해당하는 유저 정보를 응답으로 반환한다.")
        @Test
        void return_user_info_when_id_exist(){

            //given
            UserRequestDto requestDto = new UserRequestDto(
                    "sangdon",
                    "dori@dori.com",
                    "1998-02-21",
                    "MALE"
            );

            ResponseEntity<ApiResponse<UserResponseDto>> signUpResponse = testRestTemplate.exchange(
                    "/api/v1/users",
                    HttpMethod.POST,
                    new HttpEntity<>(requestDto),
                    new ParameterizedTypeReference<ApiResponse<UserResponseDto>>() {}
            );
            assertThat(signUpResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

            //when
            ResponseEntity<ApiResponse<UserResponseDto>> response =
                    testRestTemplate.exchange(
                            "/api/v1/users/me?id=" + requestDto.id(),
                            HttpMethod.GET,
                            null,
                            new ParameterizedTypeReference<ApiResponse<UserResponseDto>>() {}
                    );

            assertAll(
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                    () -> assertThat(response.getBody().data().id()).isEqualTo("sangdon"),
                    () -> assertThat(response.getBody().data().gender()).isEqualTo("MALE"),
                    () -> assertThat(response.getBody().data().birthDate()).isEqualTo("1998-02-21"),
                    () -> assertThat(response.getBody().data().email()).isEqualTo("dori@dori.com")
            );

        }

        @DisplayName("존재하지 않는 ID 로 조회할 경우, 404 Not Found 응답을 반환한다")
        @Test
        void return_404_when_id_not_exist(){
            //given
            String id = "notExistId";
            //when
            ResponseEntity<ApiResponse<UserResponseDto>> response = testRestTemplate.exchange(
                    "/api/v1/users/me?id=" + id,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<ApiResponse<UserResponseDto>>() {}
            );
            //then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }

        @DisplayName("회원 가입 시 Point 생성이 실패하면, 한 트랜젝션에 있는 User 생성도 롤백된다.")
        @Test
        void data_inconsistency_when_point_creation_fails() {
            //given
            UserRequestDto requestDto = new UserRequestDto(
                    "testUser",
                    "test@test.com",
                    "1998-02-21",
                    "MALE"
            );

            doThrow(new RuntimeException("Point 생성 실패"))
                    .when(pointService).create(anyString());

            //when
            try {
                testRestTemplate.exchange(
                        "/api/v1/users",
                        HttpMethod.POST,
                        new HttpEntity<>(requestDto),
                        new ParameterizedTypeReference<ApiResponse<UserResponseDto>>() {}
                );
            } catch (Exception e) {
            }

            //then
            assertAll(
                    () -> assertThat(userJpaRepository.findByUserId("testUser")).isEmpty(), // User는 저장되지 않음
                    () -> assertThat(pointJpaRepository.findByUserId("testUser")).isEmpty()  // Point 또한 저장되지 않음
            );
        }
    }
}
