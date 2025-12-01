package com.loopers.interfaces.api.point;

import com.loopers.domain.point.Point;
import com.loopers.domain.point.PointRepository;
import com.loopers.domain.user.User;
import com.loopers.domain.user.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PointV1ControllerTest {

    private static final String GET_USER_POINT_ENDPOINT = "/api/v1/points";
    private static final String POST_USER_POINT_ENDPOINT = "/api/v1/points/charge";

    @Autowired
    private PointRepository pointRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private DatabaseCleanUp databaseCleanUp;

    @Autowired
    private TestRestTemplate testRestTemplate;

    @AfterEach
    void tearDown() {
        databaseCleanUp.truncateAllTables();
    }

    @DisplayName("GET /api/v1/points")
    @Nested
    class UserPoint {

        @DisplayName("포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.")
        @Test
        void returnPoint_whenValidUserIdIsProvided() {
            //given
            String id = "yh45g";
            String email = "yh45g@loopers.com";
            String birth = "1994-12-05";
            String gender = "MALE";
            Long amount = 1000L;

            userRepository.save(new User(id, email, birth, gender));
            pointRepository.save(Point.create(id, amount));

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", id);

            //when
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
                    testRestTemplate.exchange(GET_USER_POINT_ENDPOINT, HttpMethod.GET, new HttpEntity<>(null, headers), responseType);

            //then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo(id),
                    () -> assertThat(response.getBody().data().amount()).isEqualTo(1000L)
            );
        }

        @DisplayName("해당 ID의 회원이 존재하지 않을 경우, null을 반환한다.")
        @Test
        void returnNull_whenUserIdExists() {
            //given
            String id = "yh45g";

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", id);

            //when
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
                    testRestTemplate.exchange(GET_USER_POINT_ENDPOINT, HttpMethod.GET, new HttpEntity<>(null, headers), responseType);

            //then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getBody().data()).isNull()
            );
        }
    }

    @DisplayName("POST /api/v1/points/charge")
    @Nested
    class Charge {

        @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
        @Test
        void returnsTotalPoint_whenChargeUserPoint() {
            //given
            String id = "yh45g";
            String email = "yh45g@loopers.com";
            String birth = "1994-12-05";
            String gender = "MALE";

            userRepository.save(new User(id, email, birth, gender));
            pointRepository.save(Point.create(id, 0L));

            PointV1Dto.ChargePointRequest request = new PointV1Dto.ChargePointRequest(id, 1000L);

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", id);

            //when
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
                    testRestTemplate.exchange(POST_USER_POINT_ENDPOINT, HttpMethod.PATCH, new HttpEntity<>(request, headers), responseType);

            //then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
                    () -> assertThat(response.getBody().data().userId()).isEqualTo(id),
                    () -> assertThat(response.getBody().data().amount()).isEqualTo(1000L)
            );
        }

        @DisplayName("존재하지 않는 유저로 요청할 경우, 404 Not Found 응답을 반환한다.")
        @Test
        void throwsException_whenInvalidUserIdIsProvided() {
            //given
            String id = "yh45g";

            HttpHeaders headers = new HttpHeaders();
            headers.add("X-USER-ID", id);

            //when
            ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType = new ParameterizedTypeReference<>() {};
            ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
                    testRestTemplate.exchange(POST_USER_POINT_ENDPOINT, HttpMethod.PATCH, new HttpEntity<>(null, headers), responseType);

            //then
            assertAll(
                    () -> assertTrue(response.getStatusCode().is4xxClientError()),
                    () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
            );
        }
    }
}
