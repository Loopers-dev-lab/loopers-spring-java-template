/**
 * 주문 생성 API 부하 테스트
 * 일정 부하 유지 시나리오 (50 req/s, 5분)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { getAuthHeaders } from '../../utils/helpers.js';
import { generateOrderRequest, getRandomUserId } from '../../utils/data-generator.js';
import { COMMERCE_API_BASE } from '../../config/base.js';
import { constantLoadScenario, defaultThresholds } from '../../utils/scenarios.js';

const errorRate = new Rate('errors');

export const options = {
  stages: constantLoadScenario.stages,
  thresholds: defaultThresholds,
};

export default function () {
  const userId = getRandomUserId(1, 10);
  const orderRequest = generateOrderRequest({ min: 1, max: 1000 }, 1);

  const payload = JSON.stringify(orderRequest);
  const params = {
    headers: getAuthHeaders(userId),
    tags: { name: 'OrderCreate' },
    timeout: '30s',
  };

  const response = http.post(
    `${COMMERCE_API_BASE}/orders/`,
    payload,
    params
  );

  const result = check(response, {
    '주문 생성 성공 (200 또는 400)': (r) => r.status === 200 || r.status === 400,
    '응답 시간 < 5초': (r) => r.timings.duration < 5000,
    '응답 본문 존재': (r) => r.body && r.body.length > 0,
  });

  errorRate.add(!result);

  // 요청 간 간격 (RPS 조절)
  sleep(Math.random() * 0.5 + 0.5); // 0.5~1초
}

