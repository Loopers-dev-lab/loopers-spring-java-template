package com.loopers.interfaces.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopers.application.user.UserFacade;
import com.loopers.application.user.UserInfo;
import com.loopers.domain.point.Point;
import com.loopers.domain.user.Gender;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
import com.loopers.interfaces.api.point.PointDto;
import com.loopers.interfaces.api.user.UserDto;
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.hamcrest.Matchers.endsWith;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Point E2E 테스트")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PointE2ETest {

    private static final String ENDPOINT = "/api/v1/points";

    private final TestRestTemplate testRestTemplate;
    private final UserFacade userFacade;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public PointE2ETest(
            TestRestTemplate restTemplate
            , UserFacade userFacade
            , DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = restTemplate;
        this.userFacade = userFacade;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {databaseCleanUp.truncateAllTables();}

    final String validLoginId = "bobby34";
    final String validEmail = "bobby34@naver.com";
    final String validBirthday = "1994-04-08";
    final Gender validGender = Gender.MALE;
    final BigDecimal validPoint = BigDecimal.valueOf(0);

    @DisplayName("포인트 조회")
    @Nested
    class GetUserTest {

        @DisplayName("성공 케이스 : 포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환")
        @Test
        void getUserPoint_withValidFields_Success() {
            // arrange

            // 먼저 User 저장하기
            UserInfo userInfo = UserInfo.builder()
                    .loginId(validLoginId)
                    .email(validEmail)
                    .birthday(validBirthday)
                    .gender(validGender)
                    .build();

            userFacade.saveUser(userInfo);

            String requestUrl = ENDPOINT + "/";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", validLoginId);
            ParameterizedTypeReference<ApiResponse<PointDto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<PointDto.PointResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(headers), responseType);

            System.out.println("--- Response Details ---");
            System.out.println("Status Code: " + response.getStatusCode());
            System.out.println("Headers: " + response.getHeaders());
            System.out.println("Body: " + response.getBody());
            System.out.println("------------------------");

            // assert
            assertTrue(response.getStatusCode().is2xxSuccessful());
            assertNotNull(response.getBody());
            assertTrue(validPoint.compareTo(response.getBody().data().amount()) == 0);
        }

        @DisplayName("실패 케이스 : `X-USER-ID` 헤더가 없을 경우, `400 Bad Request` 응답을 반환")
        @Test
        void getUserPoint_XUserIdHeaderNotExists_BadRequest() {
            // arrange
            String requestUrl = ENDPOINT + "/";
            ParameterizedTypeReference<ApiResponse<PointDto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<PointDto.PointResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.GET, new HttpEntity<>(null), responseType);

            System.out.println("--- Response Details ---");
            System.out.println("Status Code: " + response.getStatusCode());
            System.out.println("Headers: " + response.getHeaders());
            System.out.println("Body: " + response.getBody());
            System.out.println("------------------------");

            // assert
            assertTrue(response.getStatusCode().is4xxClientError());
            assertNotNull(response.getBody());
            assertEquals(requestUrl +  " API 요청에 X-USER-ID 가 꼭 필요합니다.", response.getBody().meta().message());
        }
    }

    @DisplayName("User 포인트 충전")
    @Nested
    class AddUserTest {

        @DisplayName("성공 케이스 : 존재하는 User가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환")
        @Test
        void addUserPoint_add1000_getTotalPoints() {
            // arrange
            BigDecimal requestPoint = BigDecimal.valueOf(1000);
            PointDto.PointRequest pointRequest = PointDto.PointRequest.builder()
                    .amount(requestPoint).build();

            UserDto.CreateUserRequest cur = UserDto.CreateUserRequest.builder()
                    .loginId(validLoginId)
                    .email(validEmail)
                    .birthday(validBirthday)
                    .gender(validGender)
                    .build();

            userFacade.saveUser(UserInfo.from(cur));

            String requestUrl = ENDPOINT + "/charge";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", validLoginId);
            ParameterizedTypeReference<ApiResponse<PointDto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<PointDto.PointResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(pointRequest, headers), responseType);

            System.out.println("--- Response Details ---");
            System.out.println("Status Code: " + response.getStatusCode());
            System.out.println("Headers: " + response.getHeaders());
            System.out.println("Body: " + response.getBody());
            System.out.println("------------------------");

            assertTrue(response.getStatusCode().is2xxSuccessful());
            assertNotNull(response.getBody());
            assertTrue(requestPoint.compareTo(response.getBody().data().amount()) == 0);
        }

        @DisplayName("실패 케이스 : 존재하지 않는 User로 요청할 경우, `404 Not Found` 응답을 반환")
        @Test
        void addPoint_withNoUser_BadRequest() {
            // arrange
            BigDecimal requestPoint = BigDecimal.valueOf(1000);
            PointDto.PointRequest pointRequest = PointDto.PointRequest.builder()
                    .amount(requestPoint).build();

            // act
            String requestUrl = ENDPOINT + "/charge";
            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", validLoginId);
            ParameterizedTypeReference<ApiResponse<PointDto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};

            // act
            ResponseEntity<ApiResponse<PointDto.PointResponse>> response =
                    testRestTemplate.exchange(requestUrl, HttpMethod.POST, new HttpEntity<>(pointRequest, headers), responseType);

            System.out.println("--- Response Details ---");
            System.out.println("Status Code: " + response.getStatusCode());
            System.out.println("Headers: " + response.getHeaders());
            System.out.println("Body: " + response.getBody());
            System.out.println("------------------------");

            // assert
            assertTrue(response.getStatusCode().is4xxClientError());
            assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
            assertNotNull(response.getBody());
            assertTrue(response.getBody().meta().message().endsWith("포인트를 충전할 Point 객체를 찾을 수 없습니다."));
        }
    }
}
