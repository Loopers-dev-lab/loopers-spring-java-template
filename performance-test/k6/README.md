# k6 부하 테스트 가이드

## 📋 개요
k6를 사용하여 상품 조회 API의 실제 성능을 측정합니다.

## 🎯 측정 목표
1. **응답 시간**: p50, p95, p99 지표
2. **처리량**: 초당 요청 수 (RPS)
3. **에러율**: 실패한 요청 비율
4. **캐시 효과**: Redis 캐시 적용 전후 비교

---

## 🚀 설치 및 실행

### 1. k6 설치

**macOS:**
```bash
brew install k6
```

**Linux:**
```bash
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

**Windows:**
```bash
choco install k6
```

### 2. 애플리케이션 실행

```bash
# MySQL + Redis 실행
docker-compose -f docker/infra-compose.yml up -d mysql redis-master

# 대량 데이터 생성 (10만개)
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/01-insert-brands.sql
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/02-insert-products.sql
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/03-insert-likes.sql

# 애플리케이션 실행
./gradlew :apps:commerce-api:bootRun
```

### 3. k6 테스트 실행

```bash
# AS-IS: 인덱스 없음, 캐시 없음
# 먼저 인덱스 제거
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/08-remove-indexes.sql
k6 run performance-test/k6/product-load-test-fixed.js

# 인덱스 생성
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/04-create-indexes.sql

# likeCount 마이그레이션 (이미 완료된 경우 스킵)
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/06-migrate-like-count.sql

# TO-BE: 인덱스 + 비정규화 + 캐시
k6 run performance-test/k6/product-load-test-fixed.js
```

---

## 📊 k6 출력 결과 해석

### 주요 지표

```
http_req_duration............: avg=120ms p(95)=250ms p(99)=400ms
http_reqs....................: 12000 (200/s)
http_req_failed..............: 0.5%
product_detail_duration......: avg=50ms p(95)=100ms
product_list_duration........: avg=180ms p(95)=350ms
```

| 지표 | 의미 | 목표 |
|------|------|------|
| `avg` | 평균 응답 시간 | < 200ms |
| `p(95)` | 95%의 요청 응답 시간 | < 500ms |
| `p(99)` | 99%의 요청 응답 시간 | < 1000ms |
| `http_reqs` | 초당 처리 요청 수 (RPS) | > 100 RPS |
| `http_req_failed` | 에러율 | < 1% |

---

## 📈 AS-IS vs TO-BE 비교 예시

### AS-IS (인덱스 없음, 캐시 없음)
```
http_req_duration............: avg=850ms  p(95)=1.5s
product_list_duration........: avg=1200ms p(95)=2s
http_req_failed..............: 5.2%
```

### TO-BE (인덱스 + 비정규화 + 캐시)
```
http_req_duration............: avg=35ms   p(95)=80ms  ⬇️ 24배 향상
product_list_duration........: avg=50ms   p(95)=120ms ⬇️ 24배 향상
http_req_failed..............: 0.1%                   ⬇️ 에러율 98% 감소
```

---

## 🎨 결과 시각화

### 방법 1: k6 Cloud (무료 계정)
```bash
# k6 Cloud에 결과 업로드
k6 run --out cloud performance-test/k6/product-load-test.js
```
- 자동으로 그래프 생성
- 공유 가능한 URL 제공
- 블로그에 스크린샷 첨부 가능

### 방법 2: InfluxDB + Grafana (로컬)
```bash
# InfluxDB로 결과 전송
k6 run --out influxdb=http://localhost:8086/k6 performance-test/k6/product-load-test.js
```
- 커스텀 대시보드 가능
- 더 많은 메트릭 수집

### 방법 3: JSON 결과 분석 (간단)
```bash
k6 run --out json=performance-test/results/result.json performance-test/k6/product-load-test.js
```
- `summary.json` 파일 생성
- 블로그에 표로 정리

---

## 📝 블로그 포스팅 활용 방법

### 1. **AS-IS vs TO-BE 성능 비교표**
```markdown
| 지표 | AS-IS | TO-BE | 개선율 |
|------|-------|-------|--------|
| 평균 응답 시간 | 850ms | 35ms | **24배** |
| p95 응답 시간 | 1.5s | 80ms | **19배** |
| 초당 처리량 (RPS) | 50 | 285 | **5.7배** |
| 에러율 | 5.2% | 0.1% | **98% 감소** |
```

### 2. **k6 실행 결과 스크린샷**
- AS-IS 결과 화면 캡처
- TO-BE 결과 화면 캡처
- 나란히 비교 이미지 생성

### 3. **실전 시나리오 설명**
```markdown
## 부하 테스트 시나리오

1. **Warm-up (30초)**: 0 → 10명 사용자
2. **Ramp-up (1분)**: 10 → 50명 사용자
3. **Steady (2분)**: 50명 유지
4. **Peak (1분)**: 50 → 100명 증가
5. **Peak Hold (1분)**: 100명 유지
6. **Ramp-down (30초)**: 0명으로 감소

**총 테스트 시간**: 6분
**총 요청 수**: 약 12,000건
```

### 4. **주요 발견 사항**
```markdown
## 성능 개선 인사이트

1. **Redis 캐시 효과**
   - 상품 상세 조회: 첫 요청 100ms → 이후 1ms (캐시 히트)
   - 캐시 히트율: 95% 이상

2. **인덱스 효과**
   - 브랜드 필터링: 1.2s → 50ms (24배 향상)
   - Full Table Scan → Index Range Scan

3. **비정규화 효과**
   - 좋아요 순 정렬: COUNT 집계 제거
   - CPU 사용률: 80% → 20% 감소
```

---

## 🔥 추천 블로그 구성

```markdown
# Round 5 - 조회 성능 개선 (Index + 비정규화 + Redis Cache)

## 1. 문제 인식
- 10만개 데이터 환경에서 상품 조회 느림 (1초 이상)

## 2. 해결 방법
### 2.1 DB Index 적용
- EXPLAIN 분석 결과 (AS-IS)
- 인덱스 설계 전략
- EXPLAIN 분석 결과 (TO-BE)

### 2.2 비정규화 (likeCount)
- COUNT 집계의 문제점
- likeCount 컬럼 추가
- 동기화 로직 구현

### 2.3 Redis 캐시
- Spring Cache 적용
- TTL 전략 (5분/1분)
- 캐시 무효화 처리

## 3. 성능 측정 (k6 부하 테스트)
### 3.1 테스트 환경
- 데이터: 브랜드 100개, 상품 100,000개, 좋아요 500,000개
- 부하: 최대 100명 동시 접속

### 3.2 성능 비교
[AS-IS vs TO-BE 표]

### 3.3 k6 결과 화면
[스크린샷 2개]

## 4. 주요 인사이트
- 인덱스: 24배 향상
- 비정규화: CPU 사용량 60% 감소
- Redis 캐시: 캐시 히트 시 10배 향상

## 5. 트레이드오프
- 디스크 사용량 증가 (인덱스)
- 정합성 관리 필요 (비정규화)
- Redis 장애 대응 (캐시)

## 6. 결론
- 조회 성능 24배 향상
- DB 부하 99% 감소
- 실전 적용 가능한 최적화 전략
```

---

## ⚠️ 주의사항

1. **테스트 데이터 필수**: 최소 10만개 이상의 데이터가 있어야 의미 있는 결과
2. **애플리케이션 warm-up**: 첫 요청은 느릴 수 있으므로 warm-up 단계 필요
3. **로컬 환경**: 운영 환경이 아닌 로컬에서만 테스트
4. **Redis 실행**: 캐시 테스트 시 Redis가 반드시 실행 중이어야 함

---

## 🔗 참고 자료
- k6 공식 문서: https://k6.io/docs/
- Swagger to k6: https://github.com/apideck-libraries/swagger-to-k6
- k6 Cloud: https://app.k6.io/
