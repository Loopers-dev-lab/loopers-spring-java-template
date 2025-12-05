/**
 * k6 테스트 시나리오 정의
 */

/**
 * 점진적 부하 증가 시나리오
 */
export const rampUpScenario = {
  stages: [
    { duration: '30s', target: 20 },   // 30초 동안 20 req/s까지 증가
    { duration: '1m', target: 50 },    // 1분 동안 50 req/s 유지
    { duration: '1m', target: 80 },    // 1분 동안 80 req/s 유지
    { duration: '1m', target: 100 },   // 1분 동안 100 req/s 유지 (안전한 최대)
    { duration: '1m', target: 150 },   // 1분 동안 150 req/s 유지 (스트레스)
    { duration: '30s', target: 200 },  // 30초 동안 200 req/s 유지 (과부하)
    { duration: '1m', target: 50 },    // 1분 동안 50 req/s로 복구
    { duration: '30s', target: 0 },    // 30초 동안 0으로 종료
  ],
};

/**
 * 일정 부하 유지 시나리오
 */
export const constantLoadScenario = {
  stages: [
    { duration: '30s', target: 50 },   // Warm-up
    { duration: '5m', target: 50 },    // 5분간 50 req/s 유지
    { duration: '30s', target: 0 },    // Cool-down
  ],
};

/**
 * 스파이크 테스트 시나리오
 */
export const spikeScenario = {
  stages: [
    { duration: '1m', target: 20 },    // 정상: 20 req/s
    { duration: '10s', target: 200 },  // 스파이크: 200 req/s (10초)
    { duration: '1m', target: 20 },    // 정상 복귀: 20 req/s
    { duration: '10s', target: 200 },  // 스파이크 반복
    { duration: '1m', target: 20 },    // 정상 복귀
  ],
};

/**
 * 기본 임계값 설정
 */
export const defaultThresholds = {
  http_req_duration: [
    'p(95)<3000',  // 95%는 3초 이내
    'p(99)<5000',  // 99%는 5초 이내
  ],
  http_req_failed: ['rate<0.05'],     // 실패율 5% 미만
};

