package com.loopers.interfaces.api.point;

import com.loopers.application.point.PointFacade;
import com.loopers.domain.user.Gender;
import com.loopers.interfaces.api.ApiResponse;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PointV1ApiE2ETest {

	private static final String ENDPOINT_SIGN_UP = "/api/v1/users";
	private static final String ENDPOINT_GET_POINT = "/api/v1/points";
	private static final String ENDPOINT_CHARGE_POINT = "/api/v1/points/charge";

	private final TestRestTemplate testRestTemplate;
	private final DatabaseCleanUp databaseCleanUp;
	private final PointFacade pointFacade;

	@Autowired
	public PointV1ApiE2ETest(
			TestRestTemplate testRestTemplate,
			DatabaseCleanUp databaseCleanUp,
			PointFacade pointFacade
	) {
		this.testRestTemplate = testRestTemplate;
		this.databaseCleanUp = databaseCleanUp;
		this.pointFacade = pointFacade;
	}

	@AfterEach
	void tearDown() {
		databaseCleanUp.truncateAllTables();
	}

	@DisplayName("GET /api/v1/points")
	@Nested
	class GetPoint {

		@DisplayName("포인트 조회에 성공할 경우, 보유 포인트를 응답으로 반환한다.")
		@Test
		void returnsPointAmount_whenPointExists() {
			// arrange - 회원 가입 (포인트 자동 생성됨)
			UserV1Dto.RegisterRequest registerRequest = new UserV1Dto.RegisterRequest(
					"testuser01",
					"test@example.com",
					"1990-01-01",
					Gender.MALE
			);
			testRestTemplate.postForEntity(ENDPOINT_SIGN_UP, registerRequest, ApiResponse.class);

			// 포인트 충전
			pointFacade.chargePoint("testuser01", 5000L);

			// HTTP 헤더 설정
			HttpHeaders headers = new HttpHeaders();
			headers.set("X-USER-ID", "testuser01");

			// act
			ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType =
					new ParameterizedTypeReference<>() {};
			ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
					testRestTemplate.exchange(
							ENDPOINT_GET_POINT,
							HttpMethod.GET,
							new HttpEntity<>(headers),
							responseType
					);

			// assert
			assertAll(
					() -> assertTrue(response.getStatusCode().is2xxSuccessful()),
					() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
					() -> assertThat(response.getBody()).isNotNull(),
					() -> assertThat(response.getBody().data()).isNotNull(),
					() -> assertThat(response.getBody().data().userId()).isEqualTo("testuser01"),
					() -> assertThat(response.getBody().data().amount()).isEqualTo(5000L)
			);
		}

		@DisplayName("회원 가입 직후 포인트 조회 시, 초기 포인트 0을 반환한다.")
		@Test
		void returnsZeroPoint_afterSignUp() {
			// arrange - 회원 가입 (포인트 자동 생성됨)
			UserV1Dto.RegisterRequest registerRequest = new UserV1Dto.RegisterRequest(
					"testuser01",
					"test@example.com",
					"1990-01-01",
					Gender.MALE
			);
			testRestTemplate.postForEntity(ENDPOINT_SIGN_UP, registerRequest, ApiResponse.class);

			// HTTP 헤더 설정
			HttpHeaders headers = new HttpHeaders();
			headers.set("X-USER-ID", "testuser01");

			// act
			ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType =
					new ParameterizedTypeReference<>() {};
			ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
					testRestTemplate.exchange(
							ENDPOINT_GET_POINT,
							HttpMethod.GET,
							new HttpEntity<>(headers),
							responseType
					);

			// assert
			assertAll(
					() -> assertTrue(response.getStatusCode().is2xxSuccessful()),
					() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
					() -> assertThat(response.getBody()).isNotNull(),
					() -> assertThat(response.getBody().data()).isNotNull(),
					() -> assertThat(response.getBody().data().userId()).isEqualTo("testuser01"),
					() -> assertThat(response.getBody().data().amount()).isEqualTo(0L)
			);
		}

		@DisplayName("X-USER-ID 헤더가 없을 경우, 400 Bad Request 응답을 반환한다.")
		@Test
		void returnsBadRequest_whenUserIdHeaderIsMissing() {
			// act - 헤더 없이 요청
			ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType =
					new ParameterizedTypeReference<>() {};
			ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
					testRestTemplate.exchange(
							ENDPOINT_GET_POINT,
							HttpMethod.GET,
							null,
							responseType
					);

			// assert
			assertAll(
					() -> assertTrue(response.getStatusCode().is4xxClientError()),
					() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
			);
		}

		@DisplayName("X-USER-ID 헤더가 빈 문자열일 경우, 400 Bad Request 응답을 반환한다.")
		@Test
		void returnsBadRequest_whenUserIdHeaderIsEmpty() {
			// arrange
			HttpHeaders headers = new HttpHeaders();
			headers.set("X-USER-ID", "");

			// act
			ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType =
					new ParameterizedTypeReference<>() {};
			ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
					testRestTemplate.exchange(
							ENDPOINT_GET_POINT,
							HttpMethod.GET,
							new HttpEntity<>(headers),
							responseType
					);

			// assert
			assertAll(
					() -> assertTrue(response.getStatusCode().is4xxClientError()),
					() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
			);
		}

		@DisplayName("X-USER-ID 헤더가 공백 문자열일 경우, 400 Bad Request 응답을 반환한다.")
		@Test
		void returnsBadRequest_whenUserIdHeaderIsBlank() {
			// arrange
			HttpHeaders headers = new HttpHeaders();
			headers.set("X-USER-ID", "   ");

			// act
			ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType =
					new ParameterizedTypeReference<>() {};
			ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
					testRestTemplate.exchange(
							ENDPOINT_GET_POINT,
							HttpMethod.GET,
							new HttpEntity<>(headers),
							responseType
					);

			// assert
			assertAll(
					() -> assertTrue(response.getStatusCode().is4xxClientError()),
					() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
			);
		}

		@DisplayName("존재하지 않는 사용자의 포인트 조회 시, 404 Not Found 응답을 반환한다.")
		@Test
		void returnsNotFound_whenUserDoesNotExist() {
			// arrange
			HttpHeaders headers = new HttpHeaders();
			headers.set("X-USER-ID", "nonexistentuser");

			// act
			ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType =
					new ParameterizedTypeReference<>() {};
			ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
					testRestTemplate.exchange(
							ENDPOINT_GET_POINT,
							HttpMethod.GET,
							new HttpEntity<>(headers),
							responseType
					);

			// assert
			assertAll(
					() -> assertTrue(response.getStatusCode().is4xxClientError()),
					() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
			);
		}
	}

	@DisplayName("POST /api/v1/points/charge")
	@Nested
	class ChargePoint {

		@DisplayName("존재하는 유저가 1000원을 충전할 경우, 충전된 보유 총량을 응답으로 반환한다.")
		@Test
		void returnsChargedAmount_whenUserExistsAndCharges1000() {
			// arrange - 회원 가입 (포인트 자동 생성됨)
			UserV1Dto.RegisterRequest registerRequest = new UserV1Dto.RegisterRequest(
					"testuser01",
					"test@example.com",
					"1990-01-01",
					Gender.MALE
			);
			testRestTemplate.postForEntity(ENDPOINT_SIGN_UP, registerRequest, ApiResponse.class);

			// 충전 요청 생성
			PointV1Dto.ChargeRequest chargeRequest = new PointV1Dto.ChargeRequest(1000L);

			// HTTP 헤더 설정
			HttpHeaders headers = new HttpHeaders();
			headers.set("X-USER-ID", "testuser01");

			// act
			ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType =
					new ParameterizedTypeReference<>() {};
			ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
					testRestTemplate.exchange(
							ENDPOINT_CHARGE_POINT,
							HttpMethod.POST,
							new HttpEntity<>(chargeRequest, headers),
							responseType
					);

			// assert
			assertAll(
					() -> assertTrue(response.getStatusCode().is2xxSuccessful()),
					() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
					() -> assertThat(response.getBody()).isNotNull(),
					() -> assertThat(response.getBody().data()).isNotNull(),
					() -> assertThat(response.getBody().data().userId()).isEqualTo("testuser01"),
					() -> assertThat(response.getBody().data().amount()).isEqualTo(1000L)
			);
		}

		@DisplayName("여러 번 충전할 경우, 누적된 총량을 응답으로 반환한다.")
		@Test
		void returnsAccumulatedAmount_whenChargedMultipleTimes() {
			// arrange - 회원 가입 및 초기 충전
			UserV1Dto.RegisterRequest registerRequest = new UserV1Dto.RegisterRequest(
					"testuser01",
					"test@example.com",
					"1990-01-01",
					Gender.MALE
			);
			testRestTemplate.postForEntity(ENDPOINT_SIGN_UP, registerRequest, ApiResponse.class);

			HttpHeaders headers = new HttpHeaders();
			headers.set("X-USER-ID", "testuser01");

			// 첫 번째 충전
			PointV1Dto.ChargeRequest firstCharge = new PointV1Dto.ChargeRequest(1000L);
			testRestTemplate.exchange(
					ENDPOINT_CHARGE_POINT,
					HttpMethod.POST,
					new HttpEntity<>(firstCharge, headers),
					new ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>>() {}
			);

			// 두 번째 충전 요청
			PointV1Dto.ChargeRequest secondCharge = new PointV1Dto.ChargeRequest(2000L);

			// act
			ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType =
					new ParameterizedTypeReference<>() {};
			ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
					testRestTemplate.exchange(
							ENDPOINT_CHARGE_POINT,
							HttpMethod.POST,
							new HttpEntity<>(secondCharge, headers),
							responseType
					);

			// assert
			assertAll(
					() -> assertTrue(response.getStatusCode().is2xxSuccessful()),
					() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK),
					() -> assertThat(response.getBody()).isNotNull(),
					() -> assertThat(response.getBody().data()).isNotNull(),
					() -> assertThat(response.getBody().data().amount()).isEqualTo(3000L)
			);
		}

		@DisplayName("존재하지 않는 유저로 요청할 경우, 404 Not Found 응답을 반환한다.")
		@Test
		void returnsNotFound_whenUserDoesNotExist() {
			// arrange
			PointV1Dto.ChargeRequest chargeRequest = new PointV1Dto.ChargeRequest(1000L);
			HttpHeaders headers = new HttpHeaders();
			headers.set("X-USER-ID", "nonexistentuser");

			// act
			ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType =
					new ParameterizedTypeReference<>() {};
			ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
					testRestTemplate.exchange(
							ENDPOINT_CHARGE_POINT,
							HttpMethod.POST,
							new HttpEntity<>(chargeRequest, headers),
							responseType
					);

			// assert
			assertAll(
					() -> assertTrue(response.getStatusCode().is4xxClientError()),
					() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND)
			);
		}

		@DisplayName("X-USER-ID 헤더가 없을 경우, 400 Bad Request 응답을 반환한다.")
		@Test
		void returnsBadRequest_whenUserIdHeaderIsMissing() {
			// arrange
			PointV1Dto.ChargeRequest chargeRequest = new PointV1Dto.ChargeRequest(1000L);

			// act
			ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType =
					new ParameterizedTypeReference<>() {};
			ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
					testRestTemplate.exchange(
							ENDPOINT_CHARGE_POINT,
							HttpMethod.POST,
							new HttpEntity<>(chargeRequest),
							responseType
					);

			// assert
			assertAll(
					() -> assertTrue(response.getStatusCode().is4xxClientError()),
					() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
			);
		}

		@DisplayName("충전 금액이 0일 경우, 400 Bad Request 응답을 반환한다.")
		@Test
		void returnsBadRequest_whenChargeAmountIsZero() {
			// arrange - 회원 가입
			UserV1Dto.RegisterRequest registerRequest = new UserV1Dto.RegisterRequest(
					"testuser01",
					"test@example.com",
					"1990-01-01",
					Gender.MALE
			);
			testRestTemplate.postForEntity(ENDPOINT_SIGN_UP, registerRequest, ApiResponse.class);

			PointV1Dto.ChargeRequest chargeRequest = new PointV1Dto.ChargeRequest(0L);
			HttpHeaders headers = new HttpHeaders();
			headers.set("X-USER-ID", "testuser01");

			// act
			ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType =
					new ParameterizedTypeReference<>() {};
			ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
					testRestTemplate.exchange(
							ENDPOINT_CHARGE_POINT,
							HttpMethod.POST,
							new HttpEntity<>(chargeRequest, headers),
							responseType
					);

			// assert
			assertAll(
					() -> assertTrue(response.getStatusCode().is4xxClientError()),
					() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
			);
		}

		@DisplayName("충전 금액이 음수일 경우, 400 Bad Request 응답을 반환한다.")
		@Test
		void returnsBadRequest_whenChargeAmountIsNegative() {
			// arrange - 회원 가입
			UserV1Dto.RegisterRequest registerRequest = new UserV1Dto.RegisterRequest(
					"testuser01",
					"test@example.com",
					"1990-01-01",
					Gender.MALE
			);
			testRestTemplate.postForEntity(ENDPOINT_SIGN_UP, registerRequest, ApiResponse.class);

			PointV1Dto.ChargeRequest chargeRequest = new PointV1Dto.ChargeRequest(-1000L);
			HttpHeaders headers = new HttpHeaders();
			headers.set("X-USER-ID", "testuser01");

			// act
			ParameterizedTypeReference<ApiResponse<PointV1Dto.PointResponse>> responseType =
					new ParameterizedTypeReference<>() {};
			ResponseEntity<ApiResponse<PointV1Dto.PointResponse>> response =
					testRestTemplate.exchange(
							ENDPOINT_CHARGE_POINT,
							HttpMethod.POST,
							new HttpEntity<>(chargeRequest, headers),
							responseType
					);

			// assert
			assertAll(
					() -> assertTrue(response.getStatusCode().is4xxClientError()),
					() -> assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST)
			);
		}
	}
}
