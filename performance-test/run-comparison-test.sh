#!/bin/bash

# =====================================================
# AS-IS vs TO-BE 성능 비교 테스트 자동화 스크립트
# =====================================================

set -e

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# MySQL 접속 정보
MYSQL_HOST="127.0.0.1"
MYSQL_PORT="3306"
MYSQL_USER="application"
MYSQL_PASS="application"
MYSQL_DB="loopers"

echo -e "${BLUE}==========================================${NC}"
echo -e "${BLUE}AS-IS vs TO-BE 성능 비교 테스트${NC}"
echo -e "${BLUE}==========================================${NC}"
echo ""

# Helper function: 애플리케이션 종료
stop_application() {
    echo "기존 애플리케이션 종료 중..."
    pkill -f "commerce-api" || true
    sleep 5
    echo -e "${GREEN}✓ 애플리케이션 종료 완료${NC}"
}

# Helper function: 애플리케이션 시작 및 대기
start_application() {
    echo "애플리케이션 시작 중..."
    ./gradlew :apps:commerce-api:bootRun > /dev/null 2>&1 &

    echo "애플리케이션이 준비될 때까지 대기 중 (최대 60초)..."
    for i in {1..60}; do
        if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
            echo -e "${GREEN}✓ 애플리케이션 준비 완료 (${i}초 소요)${NC}"
            sleep 10  # 추가로 10초 대기 (완전한 초기화)
            return 0
        fi
        sleep 1
    done

    echo -e "${RED}❌ 애플리케이션 시작 실패 (타임아웃)${NC}"
    exit 1
}

# 1. 초기 상태 확인
echo -e "${YELLOW}Step 1: 초기 환경 준비${NC}"
echo "현재 실행 중인 애플리케이션을 종료합니다..."
stop_application
echo ""

# 2. AS-IS 테스트 (인덱스 없음)
echo -e "${YELLOW}Step 2: AS-IS 테스트 (인덱스 제거)${NC}"
read -p "AS-IS 테스트를 진행하시겠습니까? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "인덱스 제거 중..."
    mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASS $MYSQL_DB < performance-test/sql/08-remove-indexes.sql

    echo "Redis 캐시 초기화..."
    docker exec redis-master redis-cli FLUSHDB

    echo "애플리케이션 재시작 (깨끗한 상태)..."
    start_application

    echo "k6 AS-IS 테스트 실행 중 (6분 소요)..."
    mkdir -p performance-test/results
    k6 run --quiet performance-test/k6/product-load-test-fixed.js 2>&1 | tee performance-test/results/as-is-result.txt

    echo -e "${GREEN}✓ AS-IS 테스트 완료${NC}"
    echo "결과: performance-test/results/as-is-result.txt"

    echo "AS-IS 테스트 완료. 애플리케이션 종료..."
    stop_application
else
    echo "AS-IS 테스트를 건너뜁니다"
fi
echo ""

# 3. TO-BE 테스트 (인덱스 적용)
echo -e "${YELLOW}Step 3: TO-BE 테스트 (인덱스 적용)${NC}"
read -p "TO-BE 테스트를 진행하시겠습니까? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "인덱스 생성 중..."
    mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASS $MYSQL_DB < performance-test/sql/04-create-indexes.sql

    echo "Redis 캐시 초기화..."
    docker exec redis-master redis-cli FLUSHDB

    echo "애플리케이션 재시작 (깨끗한 상태)..."
    start_application

    echo "k6 TO-BE 테스트 실행 중 (6분 소요)..."
    k6 run --quiet performance-test/k6/product-load-test-fixed.js 2>&1 | tee performance-test/results/to-be-result.txt

    echo -e "${GREEN}✓ TO-BE 테스트 완료${NC}"
    echo "결과: performance-test/results/to-be-result.txt"

    echo "TO-BE 테스트 완료. 최종 환경 정리..."
    echo "인덱스 복구 (운영 환경 유지)..."
    mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASS $MYSQL_DB < performance-test/sql/04-create-indexes.sql

    echo "애플리케이션 재시작 (운영 환경)..."
    stop_application
    start_application
else
    echo "TO-BE 테스트를 건너뜁니다"
fi
echo ""

# 4. 결과 비교
echo -e "${YELLOW}Step 4: 결과 비교${NC}"
if [ -f "performance-test/results/as-is-result.txt" ] && [ -f "performance-test/results/to-be-result.txt" ]; then
    echo ""
    echo -e "${BLUE}==========================================${NC}"
    echo -e "${BLUE}AS-IS (인덱스 없음)${NC}"
    echo -e "${BLUE}==========================================${NC}"
    grep -A 15 "k6 Performance Test Summary" performance-test/results/as-is-result.txt || echo "요약 정보를 찾을 수 없습니다"

    echo ""
    echo -e "${BLUE}==========================================${NC}"
    echo -e "${BLUE}TO-BE (인덱스 + 비정규화 + 캐시)${NC}"
    echo -e "${BLUE}==========================================${NC}"
    grep -A 15 "k6 Performance Test Summary" performance-test/results/to-be-result.txt || echo "요약 정보를 찾을 수 없습니다"

    echo ""
    echo -e "${GREEN}✓ 성능 비교 테스트 완료!${NC}"
    echo ""
    echo "상세 결과:"
    echo "  - AS-IS: performance-test/results/as-is-result.txt"
    echo "  - TO-BE: performance-test/results/to-be-result.txt"
fi

echo ""
echo -e "${GREEN}==========================================${NC}"
echo -e "${GREEN}테스트 완료!${NC}"
echo -e "${GREEN}==========================================${NC}"
