#!/bin/bash
# k6 테스트 실행 스크립트 (Bash)
# 사용법: ./run-k6.sh [스크립트 경로]

SCRIPT_PATH="${1:-scripts/commerce-api/order-create.js}"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
K6_PATH="$PROJECT_ROOT/tests/k6"

# 스크립트 경로 검증
if [ ! -f "$K6_PATH/$SCRIPT_PATH" ]; then
    echo "오류: 스크립트를 찾을 수 없습니다: $SCRIPT_PATH"
    echo "경로: $K6_PATH/$SCRIPT_PATH"
    exit 1
fi

echo "k6 테스트 실행 중..."
echo "스크립트: $SCRIPT_PATH"
echo ""

docker run --rm -i \
  -v "$K6_PATH:/scripts" \
  -e COMMERCE_API_BASE=http://host.docker.internal:8080/api/v1 \
  -e PG_API_BASE=http://host.docker.internal:8082/api/v1 \
  -e BASE_URL=http://host.docker.internal:8080 \
  -e PG_API_URL=http://host.docker.internal:8082 \
  grafana/k6:latest run "/scripts/$SCRIPT_PATH"

