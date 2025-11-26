/**
 * Round 5 - 상품 조회 API 부하 테스트 (수정 버전)
 *
 * 주요 수정사항:
 * 1. 실제 존재하는 상품 ID 범위로 수정
 * 2. textSummary 함수 안전성 개선 (null 체크)
 * 3. 에러 로깅 추가
 *
 * 실행 전 확인사항:
 * 1. MySQL에 대량 데이터가 생성되어 있는지 확인
 *    - 브랜드: 100개
 *    - 상품: 100,000개
 * 2. 애플리케이션 실행: ./gradlew :apps:commerce-api:bootRun
 * 3. Redis 실행: docker-compose -f docker/infra-compose.yml up -d redis-master
 *
 * 실행:
 * k6 run performance-test/k6/product-load-test-fixed.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

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

// 실제 존재하는 상품 ID 범위 (대량 데이터 생성 후)
const MIN_PRODUCT_ID = 1;
const MAX_PRODUCT_ID = 100000; // 10만개 상품 생성했다면

const MIN_BRAND_ID = 1;
const MAX_BRAND_ID = 100; // 100개 브랜드 생성했다면

export default function () {
  // 시나리오 1: 상품 목록 조회 (브랜드 필터 + 좋아요 순 정렬)
  let brandId = Math.floor(Math.random() * MAX_BRAND_ID) + MIN_BRAND_ID;
  let listRes = http.get(`${BASE_URL}/api/v1/products?brandId=${brandId}&sortType=LIKES_DESC&page=0&size=20`);

  let listCheck = check(listRes, {
    '[List] status is 200': (r) => r.status === 200,
    '[List] response time < 500ms': (r) => r.timings.duration < 500,
    '[List] has products': (r) => {
      if (r.status !== 200) return false;
      try {
        let body = JSON.parse(r.body);
        return body.data && body.data.content && body.data.content.length > 0;
      } catch (e) {
        console.error('Failed to parse list response:', e);
        return false;
      }
    },
  });

  errorRate.add(!listCheck);
  productListDuration.add(listRes.timings.duration);

  sleep(1);

  // 시나리오 2: 상품 상세 조회 (캐시 효과 측정)
  let productId = Math.floor(Math.random() * MAX_PRODUCT_ID) + MIN_PRODUCT_ID;
  let detailRes = http.get(`${BASE_URL}/api/v1/products/${productId}`);

  let detailCheck = check(detailRes, {
    '[Detail] status is 200': (r) => r.status === 200,
    '[Detail] response time < 100ms': (r) => r.timings.duration < 100,
    '[Detail] has product data': (r) => {
      if (r.status !== 200) return false;
      try {
        let body = JSON.parse(r.body);
        return body.data !== null && body.data !== undefined;
      } catch (e) {
        console.error('Failed to parse detail response:', e);
        return false;
      }
    },
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
  // 안전하게 메트릭 값 가져오기
  const getValue = (metric, key) => {
    try {
      return metric && metric.values && metric.values[key] !== undefined
        ? metric.values[key]
        : 0;
    } catch (e) {
      return 0;
    }
  };

  const httpReqDuration = data.metrics.http_req_duration || {};
  const productDetailDuration = data.metrics.product_detail_duration || {};
  const productListDuration = data.metrics.product_list_duration || {};
  const errors = data.metrics.errors || {};
  const httpReqs = data.metrics.http_reqs || {};

  let output = `
================== k6 Performance Test Summary ==================

Test Duration: ${(data.state.testRunDurationMs / 1000).toFixed(2)}s
Total Requests: ${getValue(httpReqs, 'count')}

Response Times:
  - Average: ${getValue(httpReqDuration, 'avg').toFixed(2)}ms
  - Median (p50): ${getValue(httpReqDuration, 'p(50)').toFixed(2)}ms
  - 95th Percentile: ${getValue(httpReqDuration, 'p(95)').toFixed(2)}ms
  - 99th Percentile: ${getValue(httpReqDuration, 'p(99)').toFixed(2)}ms

Product Detail API:
  - Average: ${getValue(productDetailDuration, 'avg').toFixed(2)}ms
  - p95: ${getValue(productDetailDuration, 'p(95)').toFixed(2)}ms

Product List API:
  - Average: ${getValue(productListDuration, 'avg').toFixed(2)}ms
  - p95: ${getValue(productListDuration, 'p(95)').toFixed(2)}ms

Error Rate: ${(getValue(errors, 'rate') * 100).toFixed(2)}%

=================================================================
`;
  return output;
}
