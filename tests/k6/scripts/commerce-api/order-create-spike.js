/**
 * 주문 생성 API 부하 테스트
 * 스파이크 테스트 시나리오
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { getAuthHeaders } from '../../utils/helpers.js';
import { generateOrderRequest, getRandomUserId } from '../../utils/data-generator.js';
import { COMMERCE_API_BASE } from '../../config/base.js';
import { spikeScenario, defaultThresholds } from '../../utils/scenarios.js';

const errorRate = new Rate('errors');

export const options = {
  stages: spikeScenario.stages,
  thresholds: {
    ...defaultThresholds,
    // 스파이크 테스트는 실패율 허용 범위를 조금 늘림
    http_req_failed: ['rate<0.10'], // 실패율 10% 미만
  },
};

export default function () {
  const userId = getRandomUserId(1, 10);
  const orderRequest = generateOrderRequest({ min: 1, max: 1000 }, 1);

  const payload = JSON.stringify(orderRequest);
  const params = {
    headers: getAuthHeaders(userId),
    tags: { name: 'OrderCreate' },
  };

  const response = http.post(
    `${COMMERCE_API_BASE}/orders/`,
    payload,
    params
  );

  const result = check(response, {
    '주문 생성 성공': (r) => r.status === 200 || r.status === 400,
    '응답 시간 < 10초': (r) => r.timings.duration < 10000, // 스파이크 테스트는 타임아웃 여유
  });

  errorRate.add(!result);

  sleep(Math.random() * 0.3 + 0.2); // 0.2~0.5초
}

