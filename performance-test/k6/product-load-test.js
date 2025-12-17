/**
 * Round 5 - 상품 조회 API 부하 테스트
 *
 * 실행 방법:
 * 1. k6 설치: brew install k6 (macOS) 또는 https://k6.io/docs/getting-started/installation/
 * 2. 애플리케이션 실행: ./gradlew :apps:commerce-api:bootRun
 * 3. k6 실행: k6 run performance-test/k6/product-load-test.js
 *
 * 측정 항목:
 * - AS-IS: 인덱스 없음, COUNT 집계, 캐시 없음
 * - TO-BE: 인덱스 적용, likeCount, Redis 캐시
 */

import http from 'k6/http';
import {check, sleep} from 'k6';
import {Rate, Trend} from 'k6/metrics';

// 커스텀 메트릭
let errorRate = new Rate('errors');
let productDetailDuration = new Trend('product_detail_duration');
let productListDuration = new Trend('product_list_duration');

// 부하 테스트 시나리오
export let options = {
  stages: [
    { duration: '30s', target: 10 },   // Warm-up: 0 → 10 users
    { duration: '1m', target: 50 },    // Ramp-up: 10 → 50 users
    { duration: '2m', target: 50 },    // Steady: 50 users
    { duration: '1m', target: 100 },   // Peak: 50 → 100 users
    { duration: '1m', target: 100 },   // Peak hold: 100 users
    { duration: '30s', target: 0 },    // Ramp-down: 100 → 0 users
  ],
  thresholds: {
    'http_req_duration': ['p(95)<500'], // 95%의 요청이 500ms 이내
    'errors': ['rate<0.1'],             // 에러율 10% 미만
  },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
  // 시나리오 1: 상품 목록 조회 (브랜드 필터 + 좋아요 순 정렬)
  let brandId = Math.floor(Math.random() * 10) + 1; // 1~10 랜덤 브랜드
  let listRes = http.get(`${BASE_URL}/api/v1/products?brandId=${brandId}&sortType=LIKES_DESC&page=0&size=20`);

  let listCheck = check(listRes, {
    '[List] status is 200': (r) => r.status === 200,
    '[List] response time < 500ms': (r) => r.timings.duration < 500,
    '[List] has products': (r) => JSON.parse(r.body).data.content.length > 0,
  });

  errorRate.add(!listCheck);
  productListDuration.add(listRes.timings.duration);

  sleep(1);

  // 시나리오 2: 상품 상세 조회 (캐시 효과 측정)
  let productId = Math.floor(Math.random() * 100) + 1; // 1~100 랜덤 상품
  let detailRes = http.get(`${BASE_URL}/api/v1/products/${productId}`);

  let detailCheck = check(detailRes, {
    '[Detail] status is 200': (r) => r.status === 200,
    '[Detail] response time < 100ms': (r) => r.timings.duration < 100,
    '[Detail] has product data': (r) => JSON.parse(r.body).data !== null,
  });

  errorRate.add(!detailCheck);
  productDetailDuration.add(detailRes.timings.duration);

  sleep(2);

  // 시나리오 3: 가격 순 정렬 조회
  let priceRes = http.get(`${BASE_URL}/api/v1/products?sortType=PRICE_ASC&page=0&size=20`);

  check(priceRes, {
    '[Price Sort] status is 200': (r) => r.status === 200,
    '[Price Sort] response time < 300ms': (r) => r.timings.duration < 300,
  });

  sleep(1);
}

export function handleSummary(data) {
  return {
    'performance-test/results/summary.json': JSON.stringify(data),
    'stdout': textSummary(data, { indent: ' ', enableColors: true }),
  };
}

function textSummary(data, options) {
  let output = `
================== k6 Performance Test Summary ==================

Test Duration: ${data.state.testRunDurationMs / 1000}s
Total Requests: ${data.metrics.http_reqs.values.count}

Response Times:
  - Average: ${data.metrics.http_req_duration.values.avg.toFixed(2)}ms
  - Median (p50): ${data.metrics.http_req_duration.values['p(50)'].toFixed(2)}ms
  - 95th Percentile: ${data.metrics.http_req_duration.values['p(95)'].toFixed(2)}ms
  - 99th Percentile: ${data.metrics.http_req_duration.values['p(99)'].toFixed(2)}ms

Product Detail API:
  - Average: ${data.metrics.product_detail_duration.values.avg.toFixed(2)}ms
  - p95: ${data.metrics.product_detail_duration.values['p(95)'].toFixed(2)}ms

Product List API:
  - Average: ${data.metrics.product_list_duration.values.avg.toFixed(2)}ms
  - p95: ${data.metrics.product_list_duration.values['p(95)'].toFixed(2)}ms

Error Rate: ${(data.metrics.errors.values.rate * 100).toFixed(2)}%

=================================================================
`;
  return output;
}
