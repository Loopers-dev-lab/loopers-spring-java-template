// event-driven-e2e-test.js
// 이벤트 기반 E2E 테스트: 아웃박스 패턴과 컨슈머 멱등성 검증
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

export const options = {
    scenarios: {
        // 시나리오 1: 상품 조회 이벤트 대량 발생
        product_view_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '10s', target: 50 },   // 50명이 동시에 상품 조회
                { duration: '20s', target: 100 },  // 100명으로 증가
                { duration: '10s', target: 0 },    // 종료
            ],
            exec: 'productViewScenario',
        },
        
        // 시나리오 2: 좋아요 액션 이벤트
        like_action_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '15s', target: 30 },   // 30명이 좋아요 액션
                { duration: '15s', target: 60 },   // 60명으로 증가
                { duration: '10s', target: 0 },    // 종료
            ],
            exec: 'likeActionScenario',
            startTime: '5s', // 5초 후 시작
        },
        
        // 시나리오 3: 결제 완료 이벤트
        payment_success_load: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '20s', target: 20 },   // 20명이 결제
                { duration: '10s', target: 0 },    // 종료
            ],
            exec: 'paymentSuccessScenario',
            startTime: '10s', // 10초 후 시작
        },
        
        // 시나리오 4: 멱등성 테스트 (동일한 결제 콜백 중복 전송)
        idempotency_test: {
            executor: 'constant-vus',
            vus: 10,
            duration: '30s',
            exec: 'idempotencyScenario',
            startTime: '20s', // 20초 후 시작
        }
    },
    thresholds: {
        'http_req_duration': ['p(95)<2000'],  // 95% 요청이 2초 이내
        'http_req_failed': ['rate<0.05'],     // 에러율 5% 미만
        'product_view_success': ['rate>0.95'], // 상품 조회 성공률 95% 이상
        'like_action_success': ['rate>0.95'],  // 좋아요 성공률 95% 이상
        'payment_success': ['rate>0.95'],      // 결제 성공률 95% 이상
    },
};

// 메트릭 정의
const productViewSuccess = new Rate('product_view_success');
const likeActionSuccess = new Rate('like_action_success');
const paymentSuccess = new Rate('payment_success');
const idempotencyCheck = new Rate('idempotency_check');
const eventCounter = new Counter('events_generated');

const BASE_URL = 'http://localhost:8080/api/v1';
const USERS = ['testuser1', 'testuser2', 'testuser3', 'testuser4', 'testuser5'];
const PRODUCT_IDS = [1, 2, 3, 4, 5, 6, 7, 8, 9, 10];

// 랜덤 사용자 선택
function getRandomUser() {
    return USERS[Math.floor(Math.random() * USERS.length)];
}

// 랜덤 상품 ID 선택
function getRandomProductId() {
    return PRODUCT_IDS[Math.floor(Math.random() * PRODUCT_IDS.length)];
}

// 시나리오 1: 상품 조회 (PRODUCT_VIEW 이벤트 발생)
export function productViewScenario() {
    const user = getRandomUser();
    const productId = getRandomProductId();
    
    const response = http.get(`${BASE_URL}/products/${productId}`, {
        headers: { 'X-USER-ID': user }
    });
    
    const success = check(response, {
        'product view status 200': (r) => r.status === 200,
        'product view has data': (r) => r.json('data') !== null,
    });
    
    productViewSuccess.add(success);
    eventCounter.add(1, { event_type: 'PRODUCT_VIEW' });
    
    sleep(0.1); // 100ms 간격
}

// 시나리오 2: 좋아요 액션 (LIKE_ACTION 이벤트 발생)
export function likeActionScenario() {
    const user = getRandomUser();
    const productId = getRandomProductId();
    const action = Math.random() > 0.7 ? 'UNLIKE' : 'LIKE'; // 70% LIKE, 30% UNLIKE
    
    const response = http.post(`${BASE_URL}/likes/products/${productId}`, 
        JSON.stringify({ action: action }), 
        {
            headers: { 
                'Content-Type': 'application/json',
                'X-USER-ID': user 
            }
        }
    );
    
    const success = check(response, {
        'like action status 200': (r) => r.status === 200,
        'like action success': (r) => r.json('success') === true,
    });
    
    likeActionSuccess.add(success);
    eventCounter.add(1, { event_type: 'LIKE_ACTION' });
    
    sleep(0.2); // 200ms 간격
}

// 시나리오 3: 결제 완료 (PAYMENT_SUCCESS 이벤트 발생)
export function paymentSuccessScenario() {
    const user = getRandomUser();
    const productId1 = getRandomProductId();
    const productId2 = getRandomProductId();
    
    // 1단계: 카드 결제 주문 생성
    const orderPayload = {
        items: [
            { productId: productId1, quantity: Math.floor(Math.random() * 3) + 1 },
            { productId: productId2, quantity: Math.floor(Math.random() * 2) + 1 }
        ],
        cardInfo: {
            cardType: 'SAMSUNG',
            cardNo: '1234-5678-9012-3456',
            callbackUrl: `${BASE_URL}/payments/callback`
        }
    };
    
    const orderResponse = http.post(`${BASE_URL}/orders/card`, 
        JSON.stringify(orderPayload), 
        {
            headers: { 
                'Content-Type': 'application/json',
                'X-USER-ID': user 
            }
        }
    );
    
    const orderSuccess = check(orderResponse, {
        'order creation status 200': (r) => r.status === 200,
        'order has payment info': (r) => r.json('data.paymentInfo') !== null,
    });
    
    if (orderSuccess) {
        const orderData = orderResponse.json('data');
        const transactionKey = orderData.paymentInfo.transactionKey;
        const orderId = orderData.orderId.toString();
        
        // 2단계: PG 결제 완료 콜백 (PAYMENT_SUCCESS 이벤트 발생)
        sleep(0.5); // PG 처리 시간 시뮬레이션
        
        const callbackPayload = {
            transactionKey: transactionKey,
            orderId: orderId,
            cardType: 'SAMSUNG',
            cardNo: '1234-5678-9012-3456',
            amount: Math.floor(orderData.finalTotalAmount),
            status: 'SUCCESS',
            reason: null
        };
        
        const callbackResponse = http.post(`${BASE_URL}/payments/callback`, 
            JSON.stringify(callbackPayload), 
            {
                headers: { 'Content-Type': 'application/json' }
            }
        );
        
        const callbackSuccess = check(callbackResponse, {
            'payment callback status 200': (r) => r.status === 200,
            'payment callback success': (r) => r.json('success') === true,
        });
        
        paymentSuccess.add(callbackSuccess);
        eventCounter.add(1, { event_type: 'PAYMENT_SUCCESS' });
    }
    
    sleep(1); // 1초 간격
}

// 시나리오 4: 멱등성 테스트 (동일한 콜백 중복 전송)
export function idempotencyScenario() {
    const user = getRandomUser();
    const productId = getRandomProductId();
    
    // 고정된 거래 키로 중복 콜백 테스트
    const fixedTransactionKey = `IDEMPOTENCY_TEST_${__VU}_${Math.floor(Date.now() / 10000)}`;
    const fixedOrderId = `${__VU}${Math.floor(Math.random() * 1000)}`;
    
    const callbackPayload = {
        transactionKey: fixedTransactionKey,
        orderId: fixedOrderId,
        cardType: 'SAMSUNG',
        cardNo: '1234-5678-9012-3456',
        amount: 25000,
        status: 'SUCCESS',
        reason: null
    };
    
    // 동일한 콜백을 3번 연속 전송 (멱등성 테스트)
    let firstSuccess = false;
    let subsequentResponses = [];
    
    for (let i = 0; i < 3; i++) {
        const response = http.post(`${BASE_URL}/payments/callback`, 
            JSON.stringify(callbackPayload), 
            {
                headers: { 'Content-Type': 'application/json' }
            }
        );
        
        if (i === 0) {
            firstSuccess = response.status === 200;
        } else {
            subsequentResponses.push(response.status === 200);
        }
        
        sleep(0.1); // 100ms 간격으로 중복 전송
    }
    
    // 첫 번째는 성공, 나머지도 성공해야 함 (멱등성)
    const idempotent = firstSuccess && subsequentResponses.every(success => success);
    idempotencyCheck.add(idempotent);
    
    if (idempotent) {
        eventCounter.add(1, { event_type: 'IDEMPOTENCY_TEST' });
    }
    
    sleep(2); // 2초 간격
}

// 테스트 완료 후 요약 출력
export function handleSummary(data) {
    const summary = {
        'test_duration': data.metrics.iteration_duration ? data.metrics.iteration_duration.avg : 0,
        'total_requests': data.metrics.http_reqs ? data.metrics.http_reqs.count : 0,
        'failed_requests': data.metrics.http_req_failed ? data.metrics.http_req_failed.count : 0,
        'events_generated': data.metrics.events_generated ? data.metrics.events_generated.count : 0,
        'product_view_success_rate': data.metrics.product_view_success ? data.metrics.product_view_success.rate : 0,
        'like_action_success_rate': data.metrics.like_action_success ? data.metrics.like_action_success.rate : 0,
        'payment_success_rate': data.metrics.payment_success ? data.metrics.payment_success.rate : 0,
        'idempotency_success_rate': data.metrics.idempotency_check ? data.metrics.idempotency_check.rate : 0,
    };
    
    console.log('\n=== 이벤트 기반 E2E 테스트 결과 ===');
    console.log(`총 요청 수: ${summary.total_requests}`);
    console.log(`실패 요청 수: ${summary.failed_requests}`);
    console.log(`생성된 이벤트 수: ${summary.events_generated}`);
    console.log(`상품 조회 성공률: ${(summary.product_view_success_rate * 100).toFixed(2)}%`);
    console.log(`좋아요 액션 성공률: ${(summary.like_action_success_rate * 100).toFixed(2)}%`);
    console.log(`결제 성공률: ${(summary.payment_success_rate * 100).toFixed(2)}%`);
    console.log(`멱등성 테스트 성공률: ${(summary.idempotency_success_rate * 100).toFixed(2)}%`);
    
    // 디버깅을 위한 전체 메트릭 출력
    console.log('\n=== 디버깅 정보 ===');
    console.log('Available metrics:', Object.keys(data.metrics));
    
    return {
        'stdout': JSON.stringify(summary, null, 2),
        'summary.json': JSON.stringify(data, null, 2),
    };
}