import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// ============================================================================
// k6 성능 테스트 스크립트 - 상품 목록 조회 API
// ============================================================================
// 
// 테스트 시나리오:
// 1. 최신순 정렬 조회 (LATEST)
// 2. 가격 오름차순 정렬 조회 (PRICE_ASC)
// 3. 좋아요 내림차순 정렬 조회 (LIKES_DESC)
// 4. 브랜드 필터링 조회
// 5. 페이지네이션 테스트
// 6. 좋아요 등록
//
// 실행 방법:
// 
// 기본 부하 테스트:
// k6 run k6_product_list.js
//
// 스트레스 테스트:
// TEST_MODE=stress k6 run k6_product_list.js
//
// ============================================================================

// 테스트 모드 설정 (기본: load, 스트레스: stress)
const TEST_MODE = __ENV.TEST_MODE || 'load';

const errorRate = new Rate('errors');
const responseTimeLatest = new Trend('response_time_latest');
const responseTimePriceAsc = new Trend('response_time_price_asc');
const responseTimeLikesDesc = new Trend('response_time_likes_desc');
const responseTimeBrandFilter = new Trend('response_time_brand_filter');
const responseTimePagination = new Trend('response_time_pagination');

function getTestOptions() {
    if (TEST_MODE === 'stress') {
        // 스트레스 테스트 설정
        return {
            stages: [
                { duration: '1m', target: 100 },
                { duration: '2m', target: 200 },
                { duration: '3m', target: 300 },
                { duration: '5m', target: 500 },
                { duration: '5m', target: 500 },
                { duration: '2m', target: 300 },
                { duration: '1m', target: 100 },
                { duration: '1m', target: 0 },
            ],
            thresholds: {
                http_req_duration: ['p(95)<2000', 'p(99)<5000'],
                errors: ['rate<0.10'],
                http_req_failed: ['rate<0.20'],
            },
        };
    } else {
        // 기본 부하 테스트 설정
        return {
            stages: [
                { duration: '30s', target: 50 },
                { duration: '1m', target: 100 },
                { duration: '2m', target: 100 },
                { duration: '30s', target: 50 },
                { duration: '30s', target: 0 },
            ],
            thresholds: {
                http_req_duration: ['p(95)<500', 'p(99)<1000'],
                errors: ['rate<0.01'],
                http_req_failed: ['rate<0.05'],
            },
        };
    }
}

export const options = getTestOptions();

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const API_BASE = `${BASE_URL}/api/v1/products`;
const LIKE_API_BASE = `${BASE_URL}/api/v1/like/products`;

function getRandomBrandId() {
    return Math.floor(Math.random() * 100) + 1;
}

function getRandomPage() {
    return Math.floor(Math.random() * 10);
}

function getRandomUserId() {
    const users = ['user1', 'user2'];
    return users[Math.floor(Math.random() * users.length)];
}


function buildQueryString(params) {
    const parts = [];
    for (const key in params) {
        if (params[key] !== null && params[key] !== undefined) {
            parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`);
        }
    }
    return parts.join('&');
}

// 상품 목록 조회 헬퍼 함수
function getProducts(params, metric) {
    const queryString = buildQueryString(params);
    const url = queryString ? `${API_BASE}?${queryString}` : API_BASE;
    const startTime = Date.now();
    
    const response = http.get(url, {
        tags: { name: params.sort || 'latest' },
    });
    
    const duration = Date.now() - startTime;

    if (metric) {
        metric.add(duration);
    }

    const checks = {
        'status is 200': (r) => r.status === 200,
        'response has data': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.meta &&
                       body.meta.result === 'SUCCESS' && 
                       body.data && 
                       body.data.products && 
                       Array.isArray(body.data.products);
            } catch (e) {
                return false;
            }
        },
        'response time < 1000ms': (r) => r.timings.duration < 1000,
    };
    
    const success = check(response, checks);

    if (!success && (response.status >= 500 || Math.random() < 0.01)) {
        try {
            const body = JSON.parse(response.body);
            console.error(`Request failed - URL: ${url}`);
            console.error(`Status: ${response.status}, Duration: ${response.timings.duration}ms`);
            console.error(`Response body: ${JSON.stringify(body).substring(0, 200)}`);
        } catch (e) {
            console.error(`Request failed - URL: ${url}, Status: ${response.status}, Body parse error`);
        }
    }
    
    errorRate.add(!success);
    
    return { response, success, duration };
}

// 좋아요 등록 헬퍼 함수
function recordLike(productId, loginId) {
    const url = `${LIKE_API_BASE}/${productId}`;
    
    const response = http.post(url, null, {
        headers: {
            'X-USER-ID': loginId,
            'Content-Type': 'application/json',
        },
        tags: { name: 'like_record' },
    });

    const checks = {
        'status is 200 or 404': (r) => r.status === 200 || r.status === 404,
        'response has data': (r) => {
            try {
                const body = JSON.parse(r.body);
                if (r.status === 404) {
                    return true;
                }
                return body.meta && body.meta.result === 'SUCCESS';
            } catch (e) {
                return false;
            }
        },
    };
    
    const success = check(response, checks);

    if (!success && response.status >= 500) {
        try {
            const body = JSON.parse(response.body);
            console.error(`Like record failed - URL: ${url}, Status: ${response.status}`);
            console.error(`Response body: ${JSON.stringify(body).substring(0, 200)}`);
        } catch (e) {
            console.error(`Like record failed - URL: ${url}, Status: ${response.status}, Body parse error`);
        }
    }
    
    errorRate.add(!success);
    
    return { response, success };
}

// ============================================================================
// 테스트 시나리오
// ============================================================================

export default function () {
    const sleepTime = TEST_MODE === 'stress' ? 0.1 : 1;

    const loginId = getRandomUserId();

    const iteration = __ITER || 0;
    
    // 시나리오 1: 최신순 정렬 조회 (LATEST)
    const latestParams = {
        sort: 'latest',
        page: getRandomPage(),
        size: 20,
    };
    const latestResult = getProducts(latestParams, responseTimeLatest);
    
    sleep(sleepTime);
    
    // 시나리오 2: 가격 오름차순 정렬 조회 (PRICE_ASC)
    const priceAscParams = {
        sort: 'price_asc',
        page: getRandomPage(),
        size: 20,
    };
    const priceAscResult = getProducts(priceAscParams, responseTimePriceAsc);
    
    sleep(sleepTime);
    
    // 시나리오 3: 좋아요 내림차순 정렬 조회 (LIKES_DESC)
    const likesDescParams = {
        sort: 'likes_desc',
        page: getRandomPage(),
        size: 20,
    };
    const likesDescResult = getProducts(likesDescParams, responseTimeLikesDesc);
    
    sleep(sleepTime);
    
    // 시나리오 4: 브랜드 필터링 조회
    const brandFilterParams = {
        brandId: getRandomBrandId(),
        sort: 'latest',
        page: 0,
        size: 20,
    };
    const brandFilterResult = getProducts(brandFilterParams, responseTimeBrandFilter);

    
    sleep(sleepTime);
    
    // 시나리오 5: 페이지네이션 테스트
    const pageSizes = [10, 20, 50];
    const randomPageSize = pageSizes[Math.floor(Math.random() * pageSizes.length)];
    
    const paginationParams = {
        sort: 'latest',
        page: getRandomPage(),
        size: randomPageSize,
    };
    const paginationResult = getProducts(paginationParams, responseTimePagination);

    
    sleep(sleepTime);
    
    // 시나리오 6: 브랜드 필터링 검증 테스트
    const brandId = getRandomBrandId();
    const brandValidationParams = {
        brandId: brandId,
        sort: 'latest',
        page: 0,
        size: 20,
    };
    const brandValidationResult = getProducts(brandValidationParams, responseTimeBrandFilter);
    
    if (brandValidationResult.success) {
        try {
            const body = JSON.parse(brandValidationResult.response.body);
            check(brandValidationResult.response, {
                'brand filter validation': () => {
                    if (body.data && body.data.products && body.data.products.length > 0) {
                        return body.data.products.every(product => product.brandId === brandId);
                    }
                    return true;
                },
            });
        } catch (e) {
        }
    }
    
    sleep(sleepTime);
    
    // 시나리오 7: 최신순 정렬 검증
    const latestValidationParams = { sort: 'latest', page: 0, size: 10 };
    const latestValidationResult = getProducts(latestValidationParams, responseTimeLatest);
    
    if (latestValidationResult.success) {
        try {
            const body = JSON.parse(latestValidationResult.response.body);
            if (body.data && body.data.products && body.data.products.length > 1) {
                const firstCreatedAt = new Date(body.data.products[0].createdAt);
                const secondCreatedAt = new Date(body.data.products[1].createdAt);
                check(latestValidationResult.response, {
                    'latest sort validation': () => firstCreatedAt >= secondCreatedAt,
                });
            }
        } catch (e) {
        }
    }
    
    sleep(sleepTime);
    
    // 시나리오 8: 가격 오름차순 정렬 검증
    const priceAscValidationParams = { sort: 'price_asc', page: 0, size: 10 };
    const priceAscValidationResult = getProducts(priceAscValidationParams, responseTimePriceAsc);
    
    if (priceAscValidationResult.success) {
        try {
            const body = JSON.parse(priceAscValidationResult.response.body);
            if (body.data && body.data.products && body.data.products.length > 1) {
                const firstPrice = body.data.products[0].price;
                const secondPrice = body.data.products[1].price;
                check(priceAscValidationResult.response, {
                    'price asc sort validation': () => firstPrice <= secondPrice,
                });
            }
        } catch (e) {
        }
    }
    
    sleep(sleepTime);
    
    // 시나리오 9: 좋아요 내림차순 정렬 검증
    const likesDescValidationParams = { sort: 'likes_desc', page: 0, size: 10 };
    const likesDescValidationResult = getProducts(likesDescValidationParams, responseTimeLikesDesc);
    
    if (likesDescValidationResult.success) {
        try {
            const body = JSON.parse(likesDescValidationResult.response.body);
            if (body.data && body.data.products && body.data.products.length > 1) {
                const firstLikes = body.data.products[0].likeCount;
                const secondLikes = body.data.products[1].likeCount;
                check(likesDescValidationResult.response, {
                    'likes desc sort validation': () => firstLikes >= secondLikes,
                });
            }
        } catch (e) {
        }
    }
    
    sleep(sleepTime);
    
    // 시나리오 10: 유저 좋아요 등록
    const productId = iteration + 1;
    recordLike(productId, loginId);
    
    sleep(sleepTime);
}

// ============================================================================
// 설정 함수 (테스트 시작 전 실행)
// ============================================================================

export function setup() {
    console.log(`Starting k6 test against: ${BASE_URL}`);
    console.log(`API endpoint: ${API_BASE}`);
    console.log(`Test mode: ${TEST_MODE}`);
    
    if (TEST_MODE === 'stress') {
        console.log('⚠️  STRESS TEST MODE: Testing system limits with high load');
        console.log('   - Max VUs: 500');
        console.log('   - More lenient thresholds');
        console.log('   - Faster request rate');
    } else {
        console.log('📊 LOAD TEST MODE: Normal performance testing');
        console.log('   - Max VUs: 100');
        console.log('   - Standard thresholds');
    }
    
    // 테스트용 사용자 생성 (user1, user2)
    const USER_API_BASE = `${BASE_URL}/api/v1/users`;
    
    for (let i = 1; i <= 2; i++) {
        const loginId = `user${i}`;
        const email = `user${i}@test.com`;
        const birthDate = '2000-01-01';
        const gender = 'M';
        
        const signupRequest = {
            loginId: loginId,
            email: email,
            birthDate: birthDate,
            gender: gender
        };
        
        const response = http.post(USER_API_BASE, JSON.stringify(signupRequest), {
            headers: {
                'Content-Type': 'application/json',
            },
        });
        
        if (response.status === 200 || response.status === 409) {
            console.log(`User ${loginId} ready (status: ${response.status})`);
        } else {
            console.warn(`Failed to create user ${loginId}: ${response.status}`);
        }
    }
    
    return {
        baseUrl: BASE_URL,
        apiBase: API_BASE,
        testMode: TEST_MODE,
        startTime: new Date().toISOString(),
    };
}

// ============================================================================
// 정리 함수 (테스트 종료 후 실행)
// ============================================================================

export function teardown(data) {
    console.log(`Test completed at: ${new Date().toISOString()}`);
    console.log(`Test started at: ${data.startTime}`);
}

