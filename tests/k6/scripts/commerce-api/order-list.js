/**
 * 주문 목록 조회 API 부하 테스트
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';
import { getAuthHeaders } from '../../utils/helpers.js';
import { getRandomUserId } from '../../utils/data-generator.js';
import { COMMERCE_API_BASE } from '../../config/base.js';
import { constantLoadScenario, defaultThresholds } from '../../utils/scenarios.js';

const errorRate = new Rate('errors');

export const options = {
  stages: constantLoadScenario.stages,
  thresholds: {
    ...defaultThresholds,
    http_req_duration: ['p(95)<1000'], // 조회는 더 빠르게 (1초 이내)
  },
};

export default function () {
  const userId = getRandomUserId(1, 10);

  const params = {
    headers: getAuthHeaders(userId),
    tags: { name: 'OrderList' },
  };

  const response = http.get(
    `${COMMERCE_API_BASE}/orders/`,
    params
  );

  const result = check(response, {
    '주문 목록 조회 성공': (r) => r.status === 200,
    '응답 시간 < 1초': (r) => r.timings.duration < 1000,
  });

  errorRate.add(!result);

  sleep(1);
}

