/**
 * PG Simulator 결제 API 부하 테스트
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { getAuthHeaders, generateUUID } from '../../utils/helpers.js';
import { getRandomUserId } from '../../utils/data-generator.js';
import { PG_API_BASE } from '../../config/base.js';
import { constantLoadScenario, defaultThresholds } from '../../utils/scenarios.js';

const errorRate = new Rate('errors');

export const options = {
  stages: constantLoadScenario.stages,
  thresholds: defaultThresholds,
};

export default function () {
  const userId = getRandomUserId(1, 10);
  
  const paymentRequest = {
    orderId: generateUUID(),
    cardType: 'SAMSUNG',
    cardNo: '1234-5678-9012-3456',
    amount: Math.floor(Math.random() * 50000) + 10000, // 10,000~60,000원
    callbackUrl: 'http://localhost:8080/api/v1/orders/callback',
  };

  const payload = JSON.stringify(paymentRequest);
  const params = {
    headers: getAuthHeaders(userId),
    tags: { name: 'PaymentApi' },
  };

  const response = http.post(
    `${PG_API_BASE}/payments`,
    payload,
    params
  );

  const result = check(response, {
    '결제 요청 성공 (200 또는 500)': (r) => r.status === 200 || r.status === 500, // PG는 40% 실패율
    '응답 시간 < 3초': (r) => r.timings.duration < 3000,
  });

  errorRate.add(!result);

  sleep(Math.random() * 0.5 + 0.5);
}

