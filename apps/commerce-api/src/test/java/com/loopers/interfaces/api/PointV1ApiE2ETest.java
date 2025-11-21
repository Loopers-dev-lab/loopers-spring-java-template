package com.loopers.interfaces.api;

import com.loopers.domain.point.Point;
import com.loopers.infrastructure.point.PointJpaRepository;
 
import com.loopers.utils.DatabaseCleanUp;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
 

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class PointV1ApiE2ETest {

    private final TestRestTemplate testRestTemplate;
    private final PointJpaRepository pointJpaRepository;
    private final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public PointV1ApiE2ETest(
            TestRestTemplate testRestTemplate,
            PointJpaRepository pointJpaRepository,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.testRestTemplate = testRestTemplate;
        this.pointJpaRepository = pointJpaRepository;
        this.databaseCleanUp = databaseCleanUp;
    }

    @AfterEach
    void tearDown() {databaseCleanUp.truncateAllTables(); }

    @DisplayName("포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.")
    @Test
    void return_point_when_user_exist (){
        //given
        String userId = "sangdon";
        BigDecimal expectedPointAmount = BigDecimal.valueOf(100);

        Point point = Point.builder()
                .userId(userId)
                .pointAmount(expectedPointAmount)
                .build();

        pointJpaRepository.save(point);

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", userId);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<Long>> response = testRestTemplate.exchange(
                "/api/v1/points",
                HttpMethod.GET,
                httpEntity,
                new ParameterizedTypeReference<ApiResponse<Long>>() {}
        );
        
        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().data()).isEqualTo(expectedPointAmount);
    }

    @DisplayName("X-USER-ID 헤더가 없을 경우, 400 Bad Request 응답을 반환한다.")
    @Test
    void return_400_when_header_not_exist (){
        //given

        //when
        ResponseEntity<ApiResponse<Long>> response = testRestTemplate.exchange(
                "/api/v1/points",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<Long>>() {}
        );

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
    @Test
    void return_total_point_when_charge_1000_point(){

        //given
        String userId = "sangdon";
        BigDecimal initialAmount = BigDecimal.valueOf(5000L);
        BigDecimal chargeAmount = BigDecimal.valueOf(1000L);

        Point point = Point.builder()
                .userId(userId)
                .pointAmount(initialAmount)
                .build();
        pointJpaRepository.save(point);

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", userId);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<Long>> response = testRestTemplate.exchange(
                "/api/v1/points/charge?amount=" + chargeAmount,
                HttpMethod.POST,
                httpEntity,
                new ParameterizedTypeReference<ApiResponse<Long>>() {}
        );

        //then
        assertAll(
                () -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
                () -> assertThat(response.getBody().data()).isEqualTo(6000)
        );
    }

    @DisplayName("존재하지 않는 유저로 요청할 경우, 404 Not Found 응답을 반환한다.")
    @Test
    void return_404_when_user_not_exist(){

        //given
        String userId = "notExistUser";
        long chargeAmount = 1000L;

        //when
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-USER-ID", userId);
        HttpEntity<Void> httpEntity = new HttpEntity<>(headers);

        ResponseEntity<ApiResponse<Long>> response = testRestTemplate.exchange(
                "/api/v1/points/charge?amount=" + chargeAmount,
                HttpMethod.POST,
                httpEntity,
                new ParameterizedTypeReference<ApiResponse<Long>>() {}
        );

        //then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
