// products-load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend, Gauge } from 'k6/metrics';

export const options = {
    stages: [
        { duration: '5s', target: 50 },   // 50 users
        { duration: '10s', target:100 },  // 100 users
        { duration: '10s', target: 200 }, // 200 users
        { duration: '5s', target: 0 },    // scale down
    ],
    thresholds: {
        'http_req_duration': ['p(95)<1000'],  // 95% requests < 1s
        'http_req_failed': ['rate<0.01'],     // error rate < 1%
    },
};

const BASE_URL = 'http://localhost:8080/api/v1/products';
const counter = new Counter('requests_total');
const successRate = new Rate('success_rate');
const p95Duration = new Trend('p95_duration_ms');
const rpsGauge = new Gauge('rps_current');

// 브랜드 ID 랜덤 생성
function getRandomBrandId() {
    return 1;
}

export default function () {
    const brandId = getRandomBrandId();

    // 1. 브랜드 ID 필터링 (idx_productentity_brand_id)
    let res1 = http.get(`${BASE_URL}?brandId=${brandId}&size=20&page=0`);
    check(res1, { 'brand-filter-200': (r) => r.status === 200 });
    successRate.add(res1.status === 200);
    p95Duration.add(res1.timings.duration);
    counter.add(1, { endpoint: 'brand-filter' });

    // 2. 브랜드 + 좋아요순 (idx_productentity_brand_id_like_count)
    let res2 = http.get(`${BASE_URL}?brandId=${brandId}&size=20&page=0&sort=likeCount,desc`);
    check(res2, { 'brand-like-200': (r) => r.status === 200 });
    successRate.add(res2.status === 200);
    p95Duration.add(res2.timings.duration);
    counter.add(1, { endpoint: 'brand-like' });

    // 3. 전체 좋아요순 (idx_productentity_like_count)
    let res3 = http.get(`${BASE_URL}?size=20&page=0&sort=likeCount,desc`);
    check(res3, { 'global-like-200': (r) => r.status === 200 });
    successRate.add(res3.status === 200);
    p95Duration.add(res3.timings.duration);
    counter.add(1, { endpoint: 'global-like' });

    // 4. 상품명 검색 (idx_productentity_name)
    let res4 = http.get(`${BASE_URL}?productName=Heavy&size=20&page=0`);
    check(res4, { 'name-search-200': (r) => r.status === 200 });
    successRate.add(res4.status === 200);
    p95Duration.add(res4.timings.duration);
    counter.add(1, { endpoint: 'name-search' });

    // 5. 브랜드 + 상품명 복합
    let res5 = http.get(`${BASE_URL}?brandId=${brandId}&productName=Heavy&size=20&page=0`);
    check(res5, { 'brand-name-200': (r) => r.status === 200 });
    successRate.add(res5.status === 200);
    p95Duration.add(res5.timings.duration);
    counter.add(1, { endpoint: 'brand-name' });

    // 6. 페이징 - 중간 페이지 (page=50)
    let res6 = http.get(`${BASE_URL}?brandId=${brandId}&size=20&page=50&sort=likeCount,desc`);
    check(res6, { 'paging-middle-200': (r) => r.status === 200 });
    successRate.add(res6.status === 200);
    p95Duration.add(res6.timings.duration);
    counter.add(1, { endpoint: 'paging-middle' });

    // 7. 대용량 페이지
    let res7 = http.get(`${BASE_URL}?size=100&page=0&sort=likeCount,desc`);
    check(res7, { 'large-page-200': (r) => r.status === 200 });
    successRate.add(res7.status === 200);
    p95Duration.add(res7.timings.duration);
    counter.add(1, { endpoint: 'large-page' });

    sleep(0.1); // 100ms 간격
}

// 인덱스별 세부 테스트 (별도 실행용)
export function indexBenchmark() {
    const brandId = getRandomBrandId();

    // 인덱스별 순차 실행 (한 번에 하나씩)
    const scenarios = [
        `${BASE_URL}?brandId=${brandId}&size=20&page=0`,                    // brand_id
        `${BASE_URL}?brandId=${brandId}&size=20&page=0&sort=likeCount,desc`, // brand_like
        `${BASE_URL}?size=20&page=0&sort=likeCount,desc`,                    // like_count
        `${BASE_URL}?productName=테스트&size=20&page=0`,                     // name
    ];

    scenarios.forEach((url, idx) => {
        let res = http.get(url);
        console.log(`Index ${idx + 1}: ${res.timings.duration}ms`);
    });
}
