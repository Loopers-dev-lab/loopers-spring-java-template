package com.loopers.interfaces.api;

import com.loopers.domain.user.Gender;
import com.loopers.interfaces.api.user.UserDto;
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
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("User E2E 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class UserE2ETest {

    private static final String ENDPOINT = "/api/v1/users";

    private final TestRestTemplate testRestTemplate;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public UserE2ETest(
            TestRestTemplate restTemplate
            , DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = restTemplate;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {databaseCleanUp.truncateAllTables();}

    final String validLoginId = "bobby34";
    final String validEmail = "bobby34@naver.com";
    final String validBirthday = "1994-04-08";
    final Gender validGender = Gender.MALE;

    @DisplayName("User 엔티티 생성")
    @Nested
    class CreateUserTest {

        @DisplayName("성공 케이스 : 회원 가입이 성공해서 생성된 유저 정보를 응답으로 반환")
        @Test
        void saveUser_returnUserResponse_Success() {
            // arrange
            UserDto.CreateUserRequest request = UserDto.CreateUserRequest.builder()
                    .loginId(validLoginId)
                    .email(validEmail)
                    .birthday(validBirthday)
                    .gender(validGender)
                    .build();

            String requestUrl = ENDPOINT + "/";
            HttpEntity<UserDto.CreateUserRequest> requestEntity = new HttpEntity<>(request);
            ParameterizedTypeReference<ApiResponse<UserDto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<UserDto.UserResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, requestEntity, responseType);

            // assert
            assertTrue(response.getStatusCode().is2xxSuccessful());
            assertNotNull(response.getBody());
            assertAll(
                    () -> assertEquals(validLoginId, response.getBody().data().loginId())
                    , () -> assertEquals(validEmail, response.getBody().data().email())
                    , () -> assertEquals(validBirthday, response.getBody().data().birthday())
                    , () -> assertEquals(validGender, response.getBody().data().gender())
            );
        }

        @DisplayName("실패 케이스 : 회원 가입 시 성별 없을 경우 `400 Bad Request` 응답 반환")
        @Test
        void saveUser_noGender_BadRequest() {
            // arrange
            UserDto.CreateUserRequest request = UserDto.CreateUserRequest.builder()
                    .loginId(validLoginId)
                    .email(validEmail)
                    .birthday(validBirthday)
                    .build();

            String requestUrl = ENDPOINT + "/";
            HttpEntity<UserDto.CreateUserRequest> requestEntity = new HttpEntity<>(request);
            ParameterizedTypeReference<ApiResponse<UserDto.UserResponse>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<UserDto.UserResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, requestEntity, responseType, request);

            // assert
            assertTrue(response.getStatusCode().is4xxClientError());
            assertNotNull(response.getBody());
            assertAll(
                    () -> assertEquals(ApiResponse.Metadata.Result.FAIL, response.getBody().meta().result())
                    , () -> assertEquals("Bad Request", response.getBody().meta().errorCode())
                    , () -> assertEquals("User : gender가 NULL일 수 없습니다.", response.getBody().meta().message())
            );
        }
    }

}
