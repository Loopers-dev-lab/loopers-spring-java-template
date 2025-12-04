import http from 'k6/http';
import {check, group} from 'k6';

export const options = {
    stages: [
        {duration: '10s', target: 1000},   // 1000명까지 10초에 증가
        {duration: '30s', target: 1000},   // 1000명 유지 30초
        {duration: '10s', target: 0},    // 10초에 감소
    ],
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],
        http_req_failed: ['rate<0.1'],
    },
};

const BASE_URL = 'http://localhost:8080';

export default function () {
    group('기본 상품 상세 조회', () => {
        const randomId = Math.floor(Math.random() * 1001) + 1;
        const response = http.get(`${BASE_URL}/api/v1/products/${randomId}/detail`);
        check(response, {
            'status는 200': (r) => r.status === 200,
            '응답 시간 < 500ms': (r) => r.timings.duration < 500,
        });
    });
}
