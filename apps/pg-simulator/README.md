## PG-Simulator (PaymentGateway)

### Description
Loopback BE 과정을 위해 PaymentGateway 를 시뮬레이션하는 App Module 입니다.
`local` 프로필로 실행 권장하며, 커머스 서비스와의 동시 실행을 위해 서버 포트가 조정되어 있습니다.
- server port : 8082
- actuator port : 8083

### Getting Started
부트 서버를 아래 명령어 혹은 `intelliJ` 통해 실행해주세요.
```shell
./gradlew :apps:pg-simulator:bootRun
```

API 는 아래와 같이 주어지니, 커머스 서비스와 동시에 실행시킨 후 진행해주시면 됩니다.
- 결제 요청 API
- 결제 정보 확인 `by transactionKey`
- 결제 정보 목록 조회 `by orderId`

```http request
### 결제 요청
POST {{pg-simulator}}/api/v1/payments
X-USER-ID: 135135
Content-Type: application/json

{
  "orderId": "1351039135",
  "cardType": "SAMSUNG",
  "cardNo": "1234-5678-9814-1451",
  "amount" : "5000",
  "callbackUrl": "http://localhost:8080/api/v1/examples/callback"
}

### 결제 정보 확인
GET {{pg-simulator}}/api/v1/payments/20250816:TR:9577c5
X-USER-ID: 135135

### 주문에 엮인 결제 정보 조회
GET {{pg-simulator}}/api/v1/payments?orderId=1351039135
X-USER-ID: 135135

```