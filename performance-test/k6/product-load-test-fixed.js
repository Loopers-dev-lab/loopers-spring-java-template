import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

// 커스텀 메트릭
const errors = new Rate('errors');
const productDetailDuration = new Rate('product_detail_duration');
const productListDuration = new Rate('product_list_duration');

// 테스트 설정
export const options = {
    stages: [
        { duration: '1m', target: 10 },   // Ramp-up: 1분 동안 10 VU로 증가
        { duration: '3m', target: 10 },   // Stable: 3분 동안 10 VU 유지
        { duration: '1m', target: 20 },   // Peak: 1분 동안 20 VU로 증가
        { duration: '1m', target: 0 },    // Ramp-down: 1분 동안 0 VU로 감소
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'], // 95%의 요청이 500ms 이하
        errors: ['rate<0.1'],              // 에러율 10% 이하
    },
};

const BASE_URL = 'http://localhost:8080';

// 랜덤 브랜드 ID (1-100)
function getRandomBrandId() {
    return Math.floor(Math.random() * 100) + 1;
}

// 랜덤 상품 ID (1-100000)
function getRandomProductId() {
    return Math.floor(Math.random() * 100000) + 1;
}

export default function () {
    // 1. 브랜드별 상품 목록 조회 (페이징)
    const brandId = getRandomBrandId();
    const page = Math.floor(Math.random() * 10); // 0-9 페이지

    let listRes = http.get(`${BASE_URL}/api/v1/products?brandId=${brandId}&page=${page}&size=20`);
    let listCheck = check(listRes, {
        '[List] status is 200': (r) => r.status === 200,
        '[List] response time < 500ms': (r) => r.timings.duration < 500,
        '[List] has products': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data && body.data.content;
            } catch (e) {
                return false;
            }
        },
    });

    errors.add(!listCheck);
    productListDuration.add(listRes.timings.duration);

    sleep(0.5);

    // 2. 상품 상세 조회
    const productId = getRandomProductId();
    let detailRes = http.get(`${BASE_URL}/api/v1/products/${productId}`);
    let detailCheck = check(detailRes, {
        '[Detail] status is 200': (r) => r.status === 200,
        '[Detail] response time < 100ms': (r) => r.timings.duration < 100,
        '[Detail] has product data': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data && body.data.id;
            } catch (e) {
                return false;
            }
        },
    });

    errors.add(!detailCheck);
    productDetailDuration.add(detailRes.timings.duration);

    sleep(0.5);

    // 3. 가격순 정렬 조회
    const sortPage = Math.floor(Math.random() * 10);
    let sortRes = http.get(`${BASE_URL}/api/v1/products?page=${sortPage}&size=20&sort=price`);
    check(sortRes, {
        '[Price Sort] status is 200': (r) => r.status === 200,
        '[Price Sort] response time < 300ms': (r) => r.timings.duration < 300,
    });

    sleep(1);
}

// 테스트 종료 후 요약 출력
export function handleSummary(data) {
    const httpReqs = data.metrics.http_reqs.values.count;
    const duration = data.state.testRunDurationMs / 1000;

    const avgDuration = data.metrics.http_req_duration.values.avg;
    const p50Duration = data.metrics.http_req_duration.values.med;
    const p95Duration = data.metrics.http_req_duration.values['p(95)'];
    const p99Duration = data.metrics.http_req_duration.values['p(99)'];

    const productDetailAvg = data.metrics.product_detail_duration ? data.metrics.product_detail_duration.values.avg : 0;
    const productDetailP95 = data.metrics.product_detail_duration ? data.metrics.product_detail_duration.values['p(95)'] : 0;

    const productListAvg = data.metrics.product_list_duration ? data.metrics.product_list_duration.values.avg : 0;
    const productListP95 = data.metrics.product_list_duration ? data.metrics.product_list_duration.values['p(95)'] : 0;

    const errorRate = data.metrics.errors ? data.metrics.errors.values.rate * 100 : 0;

    return {
        'stdout': `
================== k6 Performance Test Summary ==================

Test Duration: ${duration.toFixed(2)}s
Total Requests: ${httpReqs}

Response Times:
  - Average: ${avgDuration.toFixed(2)}ms
  - Median (p50): ${p50Duration.toFixed(2)}ms
  - 95th Percentile: ${p95Duration.toFixed(2)}ms
  - 99th Percentile: ${p99Duration.toFixed(2)}ms

Product Detail API:
  - Average: ${productDetailAvg.toFixed(2)}ms
  - p95: ${productDetailP95.toFixed(2)}ms

Product List API:
  - Average: ${productListAvg.toFixed(2)}ms
  - p95: ${productListP95.toFixed(2)}ms

Error Rate: ${errorRate.toFixed(2)}%

=================================================================
`,
    };
}
