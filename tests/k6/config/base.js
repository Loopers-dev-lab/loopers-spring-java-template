/**
 * k6 기본 설정
 */

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const PG_API_URL = __ENV.PG_API_URL || 'http://localhost:8082';
export const DEFAULT_USER_ID = __ENV.USER_ID || '1';

export const COMMERCE_API_BASE = `${BASE_URL}/api/v1`;
export const PG_API_BASE = `${PG_API_URL}/api/v1`;

