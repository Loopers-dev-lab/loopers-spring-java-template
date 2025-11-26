#!/bin/bash

# Round 5 성능 테스트 자동 실행 스크립트
# 사용법: ./performance-test/run-performance-test.sh

set -e  # 에러 발생 시 중단

echo "=========================================="
echo "Round 5 - 조회 성능 개선 테스트"
echo "=========================================="
echo ""

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# MySQL 접속 정보
MYSQL_HOST="127.0.0.1"
MYSQL_PORT="3306"
MYSQL_USER="application"
MYSQL_PASS="application"
MYSQL_DB="loopers"

# 1. Docker 확인
echo -e "${YELLOW}Step 1: Docker 컨테이너 확인${NC}"
if ! docker ps | grep -q mysql; then
    echo -e "${RED}MySQL 컨테이너가 실행 중이 아닙니다.${NC}"
    echo "실행 중..."
    docker-compose -f docker/infra-compose.yml up -d mysql redis-master
    echo "MySQL 시작 대기 중 (30초)..."
    sleep 30
else
    echo -e "${GREEN}✓ MySQL 실행 중${NC}"
fi

if ! docker ps | grep -q redis; then
    echo -e "${YELLOW}Redis 컨테이너 실행 중...${NC}"
    docker-compose -f docker/infra-compose.yml up -d redis-master
    sleep 5
else
    echo -e "${GREEN}✓ Redis 실행 중${NC}"
fi
echo ""

# 2. 테스트 데이터 확인
echo -e "${YELLOW}Step 2: 테스트 데이터 확인${NC}"
PRODUCT_COUNT=$(mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASS $MYSQL_DB -se "SELECT COUNT(*) FROM products;" 2>/dev/null || echo "0")

if [ "$PRODUCT_COUNT" -lt 10000 ]; then
    echo -e "${RED}테스트 데이터가 부족합니다 (현재: $PRODUCT_COUNT개). 생성합니다...${NC}"

    echo "  → 브랜드 100개 생성 중..."
    mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASS $MYSQL_DB < performance-test/sql/01-insert-brands.sql

    echo "  → 상품 100,000개 생성 중 (1-2분 소요)..."
    mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASS $MYSQL_DB < performance-test/sql/02-insert-products.sql

    echo "  → 좋아요 500,000개 생성 중 (2-3분 소요)..."
    mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASS $MYSQL_DB < performance-test/sql/03-insert-likes.sql

    echo -e "${GREEN}✓ 테스트 데이터 생성 완료${NC}"
else
    echo -e "${GREEN}✓ 테스트 데이터 존재 ($PRODUCT_COUNT개 상품)${NC}"
fi
echo ""

# 3. 데이터 확인
echo -e "${YELLOW}Step 3: 데이터 통계${NC}"
mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASS $MYSQL_DB -e "
SELECT 'Brands' AS table_name, COUNT(*) AS count FROM brands
UNION ALL
SELECT 'Products', COUNT(*) FROM products
UNION ALL
SELECT 'Likes', COUNT(*) FROM likes;
"
echo ""

# 4. 애플리케이션 확인
echo -e "${YELLOW}Step 4: 애플리케이션 확인${NC}"
if curl -s http://localhost:8080/actuator/health > /dev/null 2>&1; then
    echo -e "${GREEN}✓ 애플리케이션 실행 중${NC}"
else
    echo -e "${RED}애플리케이션이 실행 중이 아닙니다.${NC}"
    echo "다음 명령어로 실행하세요:"
    echo "  ./gradlew :apps:commerce-api:bootRun"
    exit 1
fi
echo ""

# 5. AS-IS 테스트 (인덱스 없음)
echo -e "${YELLOW}Step 5: AS-IS 성능 테스트 (인덱스 없음)${NC}"
read -p "AS-IS 테스트를 실행하시겠습니까? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "인덱스 제거 중..."
    mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASS $MYSQL_DB -e "
    DROP INDEX IF EXISTS idx_products_brand_id ON products;
    DROP INDEX IF EXISTS idx_products_deleted_at ON products;
    DROP INDEX IF EXISTS idx_products_brand_deleted ON products;
    DROP INDEX IF EXISTS idx_products_created_at ON products;
    DROP INDEX IF EXISTS idx_products_price ON products;
    DROP INDEX IF EXISTS idx_likes_product_id ON likes;
    DROP INDEX IF EXISTS idx_likes_product_deleted ON likes;
    DROP INDEX IF EXISTS idx_likes_user_product ON likes;
    " 2>/dev/null || true

    echo "k6 테스트 실행 중 (6분 소요)..."
    mkdir -p performance-test/results
    k6 run performance-test/k6/product-load-test-fixed.js 2>&1 | tee performance-test/results/as-is-result.txt

    echo -e "${GREEN}✓ AS-IS 테스트 완료${NC}"
    echo "결과: performance-test/results/as-is-result.txt"
fi
echo ""

# 6. TO-BE 테스트 (인덱스 적용)
echo -e "${YELLOW}Step 6: TO-BE 성능 테스트 (인덱스 + 비정규화 + 캐시)${NC}"
read -p "TO-BE 테스트를 실행하시겠습니까? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "인덱스 생성 중..."
    mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASS $MYSQL_DB < performance-test/sql/04-create-indexes.sql

    echo "likeCount 마이그레이션 중..."
    mysql -h $MYSQL_HOST -P $MYSQL_PORT -u $MYSQL_USER -p$MYSQL_PASS $MYSQL_DB < performance-test/sql/06-migrate-like-count.sql

    echo "k6 테스트 실행 중 (6분 소요)..."
    k6 run performance-test/k6/product-load-test-fixed.js 2>&1 | tee performance-test/results/to-be-result.txt

    echo -e "${GREEN}✓ TO-BE 테스트 완료${NC}"
    echo "결과: performance-test/results/to-be-result.txt"
fi
echo ""

# 7. 결과 비교
echo -e "${YELLOW}Step 7: 결과 요약${NC}"
if [ -f "performance-test/results/as-is-result.txt" ] && [ -f "performance-test/results/to-be-result.txt" ]; then
    echo ""
    echo "=========================================="
    echo "AS-IS (인덱스 없음)"
    echo "=========================================="
    grep -A 10 "k6 Performance Test Summary" performance-test/results/as-is-result.txt | head -15

    echo ""
    echo "=========================================="
    echo "TO-BE (인덱스 + 비정규화 + 캐시)"
    echo "=========================================="
    grep -A 10 "k6 Performance Test Summary" performance-test/results/to-be-result.txt | head -15

    echo ""
    echo -e "${GREEN}✓ 성능 테스트 완료!${NC}"
    echo ""
    echo "상세 결과:"
    echo "  - AS-IS: performance-test/results/as-is-result.txt"
    echo "  - TO-BE: performance-test/results/to-be-result.txt"
fi

echo ""
echo -e "${GREEN}=========================================="
echo "테스트 완료!"
echo -e "==========================================${NC}"
