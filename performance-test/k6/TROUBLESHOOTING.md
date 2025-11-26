# k6 부하 테스트 문제 해결 가이드

## 🔍 자주 발생하는 문제

### 1️⃣ **API 호출 실패 (404 Not Found)**

#### 증상
```
http_req_failed: 33.33%
[Detail] status is 200: 0% — ✓ 0 / ✗ 4989
```

#### 원인
- **테스트 데이터가 없음**: DB에 상품이 생성되지 않음
- **잘못된 ID 범위**: 1~100번 상품만 조회하는데 실제로는 없음

#### 해결 방법

**Step 1: 테스트 데이터 생성 여부 확인**
```bash
# MySQL 접속
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers

# 데이터 확인
SELECT COUNT(*) FROM brands;   -- 100개 있어야 함
SELECT COUNT(*) FROM products; -- 100,000개 있어야 함
SELECT MIN(id), MAX(id) FROM products; -- ID 범위 확인

# 없다면 생성
exit

mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/01-insert-brands.sql
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/02-insert-products.sql
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/03-insert-likes.sql
```

**Step 2: k6 스크립트 ID 범위 수정**
```javascript
// 기존 (문제)
let productId = Math.floor(Math.random() * 100) + 1; // 1~100만 조회

// 수정 (해결)
let productId = Math.floor(Math.random() * 100000) + 1; // 1~100000 조회
```

**Step 3: 수정된 스크립트 실행**
```bash
k6 run performance-test/k6/product-load-test-fixed.js
```

---

### 2️⃣ **textSummary 함수 에러**

#### 증상
```
TypeError: Cannot read property 'toFixed' of undefined or null
```

#### 원인
- k6 메트릭이 수집되지 않아 `undefined` 발생
- `data.metrics.product_detail_duration.values.avg`가 `null`

#### 해결 방법
**안전한 값 가져오기 함수 추가**
```javascript
function getValue(metric, key) {
  try {
    return metric && metric.values && metric.values[key] !== undefined
      ? metric.values[key]
      : 0;
  } catch (e) {
    return 0;
  }
}

// 사용
let avg = getValue(data.metrics.http_req_duration, 'avg').toFixed(2);
```

---

### 3️⃣ **에러율 100%**

#### 증상
```
errors: 100.00% (9978 out of 9978)
ERRO[0363] thresholds on metrics 'errors' have been crossed
```

#### 원인
- 모든 API 호출이 실패
- 애플리케이션이 실행되지 않음
- 포트가 다름 (8080이 아님)

#### 해결 방법

**Step 1: 애플리케이션 실행 확인**
```bash
# 프로세스 확인
ps aux | grep java

# 포트 확인
lsof -i :8080

# 없다면 실행
./gradlew :apps:commerce-api:bootRun
```

**Step 2: API 수동 테스트**
```bash
# 상품 목록 조회 테스트
curl -v http://localhost:8080/api/v1/products?page=0&size=5

# 상품 상세 조회 테스트
curl -v http://localhost:8080/api/v1/products/1

# 200 OK가 안 나오면 로그 확인
```

**Step 3: 포트 변경이 필요하면**
```javascript
// k6 스크립트 수정
const BASE_URL = 'http://localhost:8080'; // 실제 포트로 변경
```

---

### 4️⃣ **응답 파싱 실패**

#### 증상
```
[List] has products: 0% — ✓ 0 / ✗ 4989
```

#### 원인
- API 응답 구조가 다름
- `body.data.content`가 아닌 다른 구조

#### 해결 방법

**Step 1: 실제 응답 구조 확인**
```bash
curl http://localhost:8080/api/v1/products?page=0&size=5 | jq
```

**Step 2: 응답 예시**
```json
{
  "success": true,
  "data": {
    "content": [
      { "id": 1, "name": "상품1" }
    ],
    "totalElements": 100000
  }
}
```

**Step 3: k6 체크 로직 수정**
```javascript
check(listRes, {
  '[List] has products': (r) => {
    if (r.status !== 200) return false;
    let body = JSON.parse(r.body);
    // 실제 응답 구조에 맞게 수정
    return body.data && body.data.content && body.data.content.length > 0;
  },
});
```

---

## ✅ 정상 실행 시 예상 결과

```
✓ [List] status is 200............: 100.00% ✓ 4989 / ✗ 0
✓ [List] response time < 500ms....: 99.80%  ✓ 4979 / ✗ 10
✓ [List] has products.............: 100.00% ✓ 4989 / ✗ 0
✓ [Detail] status is 200..........: 100.00% ✓ 4989 / ✗ 0
✓ [Detail] response time < 100ms..: 95.20%  ✓ 4750 / ✗ 239
✓ [Detail] has product data.......: 100.00% ✓ 4989 / ✗ 0

http_req_duration..................: avg=35ms   p(95)=80ms
errors.............................: 0.10%
http_req_failed....................: 0.00%
```

---

## 🚀 빠른 해결 체크리스트

- [ ] **MySQL 실행 중?** `docker-compose up -d mysql`
- [ ] **Redis 실행 중?** `docker-compose up -d redis-master`
- [ ] **테스트 데이터 생성?** SQL 파일 3개 실행
- [ ] **애플리케이션 실행 중?** `./gradlew :apps:commerce-api:bootRun`
- [ ] **포트 8080 확인?** `lsof -i :8080`
- [ ] **API 수동 테스트?** `curl http://localhost:8080/api/v1/products`
- [ ] **수정된 스크립트 사용?** `product-load-test-fixed.js`

---

## 📝 테스트 실행 순서 (정석)

```bash
# 1. 인프라 실행
docker-compose -f docker/infra-compose.yml up -d mysql redis-master

# 2. 테스트 데이터 생성 (최초 1회)
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/01-insert-brands.sql
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/02-insert-products.sql
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/03-insert-likes.sql

# 3. 데이터 확인
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers -e "SELECT COUNT(*) FROM products;"

# 4. 애플리케이션 실행
./gradlew :apps:commerce-api:bootRun

# 5. API 수동 테스트 (다른 터미널)
curl http://localhost:8080/api/v1/products?page=0&size=5

# 6. k6 실행 (정상 응답 확인 후)
k6 run performance-test/k6/product-load-test-fixed.js
```

---

## 💡 추가 팁

### k6 디버깅 모드
```bash
# 상세 로그 출력
k6 run --verbose performance-test/k6/product-load-test-fixed.js

# 단일 VU로 테스트 (디버깅용)
k6 run --vus 1 --duration 10s performance-test/k6/product-load-test-fixed.js
```

### 응답 로깅
```javascript
// k6 스크립트에 추가
if (listRes.status !== 200) {
  console.error(`List API failed: ${listRes.status} - ${listRes.body}`);
}
```

### 실시간 모니터링
```bash
# Grafana k6 확장 (선택적)
k6 run --out influxdb=http://localhost:8086/k6 performance-test/k6/product-load-test-fixed.js
```

---

## 🔗 관련 문서
- k6 공식 문서: https://k6.io/docs/
- k6 트러블슈팅: https://k6.io/docs/misc/troubleshooting/
