import http from 'k6/http';
import {check, group, sleep} from 'k6';

export const options = {
    stages: [
        {duration: '10s', target: 20},   // 20명까지 10초에 증가
        {duration: '30s', target: 20},   // 20명 유지 30초
        {duration: '10s', target: 0},    // 10초에 감소
    ],
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],
        http_req_failed: ['rate<0.1'],
    },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
    // 1. 기본 상품 목록 조회
    group('기본 상품 목록 조회', () => {
        const response = http.get(`${BASE_URL}/api/v1/products`);
        check(response, {
            'status는 200': (r) => r.status === 200,
            '응답 시간 < 500ms': (r) => r.timings.duration < 500,
        });
    });

    sleep(0.2);  // 0.2초 (더 빠른 테스트)

    // 2. 브랜드별 필터 조회
    group('브랜드별 필터 조회', () => {
        const response = http.get(`${BASE_URL}/api/v1/products?brandId=1`);
        check(response, {
            'status는 200': (r) => r.status === 200,
            '응답 시간 < 500ms': (r) => r.timings.duration < 500,
        });
    });

    sleep(0.2);

    // 3. 생성일자 정렬 조회
    group('생성일자 정렬 조회', () => {
        const response = http.get(`${BASE_URL}/api/v1/products?createdAtSort=DESC`);
        check(response, {
            'status는 200': (r) => r.status === 200,
            '응답 시간 < 500ms': (r) => r.timings.duration < 500,
        });
    });

    sleep(0.2);

    // 4. 가격 정렬 조회
    group('가격 정렬 조회', () => {
        const response = http.get(`${BASE_URL}/api/v1/products?priceSort=ASC`);
        check(response, {
            'status는 200': (r) => r.status === 200,
            '응답 시간 < 500ms': (r) => r.timings.duration < 500,
        });
    });

    sleep(0.2);

    // 5. 좋아요 수 정렬 조회
    group('좋아요 수 정렬 조회', () => {
        const response = http.get(`${BASE_URL}/api/v1/products?likeCountSort=DESC`);
        check(response, {
            'status는 200': (r) => r.status === 200,
            '응답 시간 < 500ms': (r) => r.timings.duration < 500,
        });
    });

    sleep(0.2);

    // 6. 페이지네이션 조회
    group('페이지네이션 조회', () => {
        const response = http.get(`${BASE_URL}/api/v1/products?pageNo=0&pageSize=20`);
        check(response, {
            'status는 200': (r) => r.status === 200,
            '응답 시간 < 500ms': (r) => r.timings.duration < 500,
        });
    });

    sleep(0.2);

    // 7. 조합된 필터 조회
    group('조합된 필터 조회', () => {
        const response = http.get(`${BASE_URL}/api/v1/products?brandId=10&priceSort=ASC&pageNo=0&pageSize=20`);
        check(response, {
            'status는 200': (r) => r.status === 200,
            '응답 시간 < 500ms': (r) => r.timings.duration < 500,
        });
    });

    sleep(0.2);
}
