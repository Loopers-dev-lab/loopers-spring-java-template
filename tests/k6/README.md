# k6 부하 테스트

k6를 사용한 Commerce API 부하 테스트 스크립트입니다.

## 설치

### Docker 사용 (권장)

Docker를 사용하면 별도 설치 없이 바로 실행할 수 있습니다. 실행 스크립트(`run-k6.ps1` 또는 `run-k6.sh`)를 사용하거나 docker-compose를 사용하세요.

### 로컬 설치 (선택사항)

로컬에 k6를 설치하고 싶은 경우:

#### macOS
```bash
brew install k6
```

#### Windows
```bash
choco install k6
```

#### Linux
```bash
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

## 테스트 실행

### Docker를 사용한 실행 (권장)

#### 방법 1: 실행 스크립트 사용 (가장 간단)

**Windows (PowerShell):**
```powershell
cd tests/k6
.\run-k6.ps1                                    # 기본 스크립트 실행
.\run-k6.ps1 scripts/commerce-api/order-create-constant.js
.\run-k6.ps1 scripts/pg-simulator/payment-api.js
```

**Linux/Mac (Bash):**
```bash
cd tests/k6
chmod +x run-k6.sh
./run-k6.sh                                     # 기본 스크립트 실행
./run-k6.sh scripts/commerce-api/order-create-constant.js
./run-k6.sh scripts/pg-simulator/payment-api.js
```

#### 방법 2: docker-compose 사용

```bash
# 프로젝트 루트에서 실행
docker-compose -f docker/k6-compose.yml run --rm k6 run /scripts/scripts/commerce-api/order-create.js
docker-compose -f docker/k6-compose.yml run --rm k6 run /scripts/scripts/commerce-api/order-create-constant.js
docker-compose -f docker/k6-compose.yml run --rm k6 run /scripts/scripts/pg-simulator/payment-api.js
```

#### 방법 3: Docker 명령어 직접 사용

**Windows (PowerShell):**
```powershell
cd tests/k6
docker run --rm -i -v ${PWD}:/scripts -e COMMERCE_API_BASE=http://host.docker.internal:8080/api/v1 grafana/k6:latest run /scripts/scripts/commerce-api/order-create.js
```

**Linux/Mac (Bash):**
```bash
cd tests/k6
docker run --rm -i -v $(pwd):/scripts -e COMMERCE_API_BASE=http://host.docker.internal:8080/api/v1 grafana/k6:latest run /scripts/scripts/commerce-api/order-create.js
```

### 로컬 k6 설치 후 실행

로컬에 k6가 설치되어 있는 경우:

```bash
# 주문 생성 API - 점진적 부하 증가
k6 run scripts/commerce-api/order-create.js

# 주문 생성 API - 일정 부하 유지
k6 run scripts/commerce-api/order-create-constant.js

# 주문 생성 API - 스파이크 테스트
k6 run scripts/commerce-api/order-create-spike.js

# 주문 목록 조회
k6 run scripts/commerce-api/order-list.js

# PG Simulator 결제 API
k6 run scripts/pg-simulator/payment-api.js
```

### 환경 변수와 함께 실행
```bash
# 기본 URL 변경
BASE_URL=http://localhost:8080 USER_ID=1 k6 run scripts/commerce-api/order-create.js

# PG API URL 변경
PG_API_URL=http://localhost:8082 k6 run scripts/pg-simulator/payment-api.js
```

### 결과를 파일로 저장
```bash
# JSON 형식으로 저장
k6 run --out json=results/order-test-$(date +%Y%m%d-%H%M%S).json scripts/commerce-api/order-create.js

# InfluxDB로 전송 (선택사항)
k6 run --out influxdb=http://localhost:8086/k6 scripts/commerce-api/order-create.js
```

### Cloud 실행
```bash
# k6 Cloud에서 실행
k6 cloud scripts/commerce-api/order-create.js
```

## 테스트 시나리오

### 1. 점진적 부하 증가 (ramp-up)
- 20 req/s → 50 req/s → 80 req/s → 100 req/s → 150 req/s → 200 req/s
- 시스템의 최대 처리량을 찾기 위한 테스트

### 2. 일정 부하 유지 (constant load)
- 5분간 50 req/s 유지
- 시스템 안정성 확인

### 3. 스파이크 테스트
- 정상 트래픽 → 급격한 트래픽 증가 → 정상 복귀
- 시스템 복구 능력 확인

## 임계값 (Thresholds)

기본 임계값:
- `http_req_duration`: 
  - p(95): 95% 요청이 3초 이내 응답
  - p(99): 99% 요청이 5초 이내 응답
- `http_req_failed`: 실패율 5% 미만

k6는 기본적으로 모든 percentile (p50, p75, p90, p95, p99, p99.9, p99.99)을 측정하며, 결과에 모두 표시됩니다.

## 폴더 구조

```
k6/
├── scripts/              # 테스트 스크립트
│   ├── commerce-api/    # Commerce API 테스트
│   └── pg-simulator/    # PG Simulator 테스트
├── utils/               # 공통 유틸리티
│   ├── helpers.js       # 헬퍼 함수
│   ├── data-generator.js # 테스트 데이터 생성
│   └── scenarios.js     # 테스트 시나리오 정의
├── config/              # 설정 파일
│   └── base.js         # 기본 설정
├── data/                # 테스트 데이터 파일 (선택사항)
├── results/             # 테스트 결과 (gitignore)
└── README.md           # 이 파일
```

## 모니터링 연동

k6 테스트 결과를 Grafana에서 확인하려면:

1. InfluxDB에 결과 전송:
```bash
k6 run --out influxdb=http://localhost:8086/k6 scripts/commerce-api/order-create.js
```

2. Grafana에서 k6 데이터 소스 추가

## 주의사항

1. 로컬 환경에서는 초당 100 req/s 이상은 시스템에 부담을 줄 수 있습니다.
2. 실제 운영 환경 테스트 시 서비스에 영향을 주지 않도록 주의하세요.
3. 테스트 전에 인프라(MySQL, Redis 등)가 정상적으로 실행 중인지 확인하세요.

## 테스트 데이터 준비

k6 테스트를 실행하기 전에 데이터베이스에 충분한 테스트 데이터를 생성해야 합니다.

### 시드 데이터 생성

테스트에 필요한 데이터를 생성하기 위해 시드 데이터 SQL 파일을 실행합니다:

**MySQL 8.0 이상:**
```bash
mysql -u [username] -p [database_name] < tests/k6/data/seed-data.sql
```

**MySQL 5.7:**
```bash
mysql -u [username] -p [database_name] < tests/k6/data/seed-data-mysql57.sql
```

또는 MySQL 클라이언트에서 직접 실행:

```sql
-- MySQL 8.0+
source tests/k6/data/seed-data.sql;

-- MySQL 5.7
source tests/k6/data/seed-data-mysql57.sql;
```

### 생성되는 데이터

- **User**: 10개 (ID: 1~10)
  - 각 사용자마다 고유한 login_id, email
- **Point**: 10개 (각 User마다 1,000,000 포인트)
- **Brand**: 100개 (ID: 1~100)
- **Product**: 1,000개 (ID: 1~1000)
  - Brand 1~100에 균등 분배
  - 가격: 10,000원부터 시작하여 100원씩 증가
- **Stock**: 1,000개 (각 Product마다 10,000,000개 재고)
- **Coupon**: 20개
  - 각 User마다 FIXED_AMOUNT(5,000원), PERCENTAGE(10%) 쿠폰 각 1개

### 요구사항

- MySQL 5.7 이상
  - MySQL 8.0 이상: `seed-data.sql` (재귀 CTE 사용, 더 효율적)
  - MySQL 5.7: `seed-data-mysql57.sql` (호환성 보장)
- k6 스크립트가 사용하는 범위와 일치:
  - User ID: 1~10
  - Product ID: 1~1000

## 트러블슈팅

### "connection refused" 에러
- Commerce API가 실행 중인지 확인: `http://localhost:8080/actuator/health`
- PG Simulator가 실행 중인지 확인: `http://localhost:8082/actuator/health`

### "NOT_FOUND" 에러 (Product 또는 User)
- 시드 데이터가 제대로 생성되었는지 확인
- `data/seed-data.sql` 스크립트를 실행했는지 확인

### 메모리 부족
- VU 수를 줄이거나 스크립트를 단순화하세요.
- `--vus` 옵션으로 동시 사용자 수를 제한하세요.

