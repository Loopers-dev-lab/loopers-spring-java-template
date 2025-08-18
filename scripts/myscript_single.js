import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 30 }, // 워밍업
        { duration: '5m',  target: 100 } // 측정
    ],
    thresholds: {
        http_req_duration: ['p(50)<100', 'p(95)<500'], // 캐시 적용 후 엄격한 기준
    }
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';

// ===== 테스트할 요청들 설정 =====
const TEST_CONFIGS = [
    {
        url: `${BASE}/api/v1/products?sort=latest&page=0&size=20`,
        description: "첫 페이지 최신순",
        weight: 50 // 50% 비율
    },
    {
        url: `${BASE}/api/v1/products?brandId=1&sort=likes_desc&page=0&size=20`,
        description: "브랜드1 인기순",
        weight: 25 // 25% 비율
    },
    {
        url: `${BASE}/api/v1/products?sort=price_asc&page=0&size=20`,
        description: "전체 가격순",
        weight: 15 // 15% 비율
    },
    {
        url: `${BASE}/api/v1/products?sort=latest&page=100&size=20`,
        description: "깊은 페이지",
        weight: 10 // 10% 비율
    }
];

// 가중치 기반 선택을 위한 누적 배열 생성
const weightedConfigs = [];
TEST_CONFIGS.forEach(config => {
    for (let i = 0; i < config.weight; i++) {
        weightedConfigs.push(config);
    }
});

export default function () {
    // 가중치 기반으로 요청 선택
    const selectedConfig = weightedConfigs[Math.floor(Math.random() * weightedConfigs.length)];

    const res = http.get(selectedConfig.url, {
        tags: {
            endpoint: 'product-list-cache',
            request_type: selectedConfig.description.replace(/\s+/g, '_').toLowerCase()
        }
    });

    check(res, {
        '상태 코드 200': (r) => r.status === 200,
        '응답 시간 < 100ms': (r) => r.timings.duration < 100,
        '응답 시간 < 500ms': (r) => r.timings.duration < 500,
        '응답에 data 필드 존재': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data && Array.isArray(body.data.items);
            } catch (e) {
                return false;
            }
        },
        '데이터 개수 확인': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data?.items?.length > 0;
            } catch (e) {
                return false;
            }
        }
    });

    // 응답 시간 상세 로깅 (2%만)
    if (Math.random() < 0.02) {
        console.log(`[${selectedConfig.description}] 응답시간: ${res.timings.duration.toFixed(2)}ms | 상태: ${res.status}`);

        if (res.status === 200) {
            try {
                const body = JSON.parse(res.body);
                const itemCount = body.data?.items?.length || 0;
                const totalCount = body.data?.totalCount || 0;
                console.log(`데이터: ${itemCount}개 조회 / 전체 ${totalCount}개`);
            } catch (e) {
                console.log('JSON 파싱 실패');
            }
        }
    }

    sleep(0.1); // 짧은 간격으로 더 많은 요청
}

// 캐시 성능 분석을 위한 추가 함수
export function handleSummary(data) {
    const duration = data.metrics.http_req_duration;

    console.log('\n=== 캐시 성능 분석 (다중 요청) ===');
    console.log(`총 요청 수: ${data.metrics.http_reqs.count}`);
    console.log(`평균 응답시간: ${duration.avg.toFixed(2)}ms`);
    console.log(`중간값: ${duration.med.toFixed(2)}ms`);
    console.log(`95%ile: ${duration['p(95)'].toFixed(2)}ms`);
    console.log(`최소: ${duration.min.toFixed(2)}ms`);
    console.log(`최대: ${duration.max.toFixed(2)}ms`);

    // 캐시 효과 분석
    const avg = duration.avg;
    let cacheStatus = '';
    if (avg < 50) {
        cacheStatus = '🚀 캐시 HIT (매우 빠름)';
    } else if (avg < 200) {
        cacheStatus = '✅ 캐시 HIT (빠름)';
    } else if (avg < 1000) {
        cacheStatus = '⚠️ 부분 캐시 HIT (보통)';
    } else {
        cacheStatus = '❌ 캐시 MISS 다수 (느림)';
    }

    console.log(`전체 캐시 상태: ${cacheStatus}`);

    // 요청 타입별 분석 (태그 기반)
    console.log('\n--- 요청 타입별 분석 ---');
    TEST_CONFIGS.forEach(config => {
        console.log(`${config.description}: ${config.weight}% 비율`);
    });

    console.log('========================\n');

    return {
        'multi-cache-performance.json': JSON.stringify({
            test_configs: TEST_CONFIGS,
            performance: {
                avg_duration: duration.avg,
                median_duration: duration.med,
                p95_duration: duration['p(95)'],
                min_duration: duration.min,
                max_duration: duration.max,
                total_requests: data.metrics.http_reqs.count,
                success_rate: (data.metrics.checks.passes / (data.metrics.checks.passes + data.metrics.checks.fails) * 100).toFixed(2) + '%'
            },
            cache_status: cacheStatus,
            timestamp: new Date().toISOString()
        }, null, 2)
    };
}