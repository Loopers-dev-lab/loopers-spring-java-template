
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 30 }, // 워밍업
        { duration: '5m',  target: 100 } // 측정
    ],
    thresholds: {
        http_req_duration: ['p(50)<100', 'p(95)<500'], // 인덱스 적용 후 더 엄격한 기준
    }
};

const BASE = __ENV.BASE_URL || 'http://localhost:8080';
const brands = Array.from({length: 40}, (_, i) => i + 1); // 1-40 (실제 브랜드 개수)
const sortOptions = ['latest', 'price_asc', 'likes_desc']; // 실제 지원하는 정렬 옵션

export default function () {
    // 상품 목록 조회만 테스트 (상세 API가 없으므로)
    const brandId = Math.random() < 0.7 ? 
        brands[Math.floor(Math.random() * brands.length)] : // 70% 특정 브랜드
        null; // 30% 전체 조회
    
    const sort = sortOptions[Math.floor(Math.random() * sortOptions.length)];
    const page = Math.floor(Math.random() * 1000); // 100만 건 기준으로 더 많은 페이지
    const size = Math.random() < 0.8 ? 20 : Math.floor(Math.random() * 50) + 1; // 80%는 기본 20개
    
    // 올바른 API 경로로 수정
    let url = `${BASE}/api/v1/products?sort=${sort}&page=${page}&size=${size}`;
    if (brandId) {
        url += `&brandId=${brandId}`;
    }
    
    const res = http.get(url, { 
        tags: { 
            endpoint: 'product-list',
            brand: brandId ? 'filtered' : 'all',
            sort: sort 
        } 
    });
    
    check(res, {
        '상태 코드 200': (r) => r.status === 200,
        '응답 시간 < 500ms': (r) => r.timings.duration < 500,
        '응답에 data 필드 존재': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.data && Array.isArray(body.data.items);
            } catch (e) {
                return false;
            }
        }
    });
    
    // 응답 분석 (선택적)
    if (res.status === 200) {
        try {
            const body = JSON.parse(res.body);
            const itemCount = body.data?.items?.length || 0;
            const totalCount = body.data?.totalCount || 0;
            
            // 로그 출력 (성능 테스트에는 영향 없도록 제한적으로)
            if (Math.random() < 0.01) { // 1%만 로깅
                console.log(`[${brandId || 'ALL'}] ${sort} - Page ${page}: ${itemCount}/${totalCount} items`);
            }
        } catch (e) {
            // JSON 파싱 실패는 무시
        }
    }
    
    sleep(0.5); // 요청 간격 단축 (더 높은 부하)
}

// 시나리오별 테스트 함수들
export function testBrandFiltering() {
    const brandId = brands[Math.floor(Math.random() * brands.length)];
    const res = http.get(`${BASE}/api/v1/products?brandId=${brandId}&sort=likes_desc&page=0&size=20`);
    check(res, { '브랜드 필터링 성공': (r) => r.status === 200 });
}

export function testSorting() {
    const sort = sortOptions[Math.floor(Math.random() * sortOptions.length)];
    const res = http.get(`${BASE}/api/v1/products?sort=${sort}&page=0&size=20`);
    check(res, { '정렬 기능 성공': (r) => r.status === 200 });
}

export function testPagination() {
    const page = Math.floor(Math.random() * 100);
    const res = http.get(`${BASE}/api/v1/products?page=${page}&size=20`);
    check(res, { '페이징 기능 성공': (r) => r.status === 200 });
}