/**
 * k6 공통 헬퍼 함수
 */

/**
 * 인증 헤더 생성
 */
export function getAuthHeaders(userId) {
  return {
    'Content-Type': 'application/json',
    'X-USER-ID': String(userId),
  };
}

/**
 * 응답 로깅 (디버깅용)
 */
export function logResponse(response, context = '') {
  console.log(`[${context}] Status: ${response.status}, Time: ${response.timings.duration}ms`);
  if (response.status >= 400) {
    console.log(`[${context}] Error: ${response.body}`);
  }
}

/**
 * UUID 생성 (간단한 버전)
 */
export function generateUUID() {
  return `${Date.now()}-${Math.random().toString(36).substring(2, 11)}`;
}

/**
 * 요청 간 랜덤 대기 (Think Time)
 */
export function randomSleep(min, max) {
  const delay = Math.floor(Math.random() * (max - min + 1) + min);
  return delay / 1000; // 초 단위로 변환
}

