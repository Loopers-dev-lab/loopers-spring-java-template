# Performance Test Guide

## 📋 개요
이 디렉토리는 Round 5 "조회 성능 개선" 과제를 위한 성능 테스트 자료를 포함합니다.

## 🎯 테스트 목표
1. **DB Index**: 브랜드 필터 + 좋아요 순 정렬 쿼리 최적화
2. **비정규화**: 좋아요 수 COUNT 집계 제거 (`likeCount` 컬럼 추가)
3. **Redis 캐시**: 조회 API 응답 속도 개선

## 📁 디렉토리 구조
```
performance-test/
├── sql/                    # 대량 테스트 데이터 생성 SQL
│   ├── 01-insert-brands.sql
│   ├── 02-insert-products.sql
│   ├── 03-insert-likes.sql
│   ├── 04-create-indexes.sql
│   └── 05-explain-queries.sql
├── results/               # 성능 측정 결과
│   ├── before-index.txt
│   └── after-index.txt
└── README.md              # 실행 가이드 (본 파일)
```

## 🚀 실행 방법

### 1. Docker MySQL 실행
```bash
cd /Users/minu/Documents/project/loopers/loopers-spring-java-template
docker-compose -f docker/infra-compose.yml up -d mysql
```

### 2. 대량 데이터 INSERT (순차 실행)
```bash
# 브랜드 100개 생성
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/01-insert-brands.sql

# 상품 100,000개 생성 (약 1-2분 소요)
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/02-insert-products.sql

# 좋아요 500,000개 생성 (약 2-3분 소요)
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/03-insert-likes.sql
```

### 3. 성능 측정 (AS-IS: 인덱스 없음)
```bash
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/05-explain-queries.sql > performance-test/results/before-index.txt
```

### 4. 인덱스 생성
```bash
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/04-create-indexes.sql
```

### 5. 성능 측정 (TO-BE: 인덱스 적용)
```bash
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers < performance-test/sql/05-explain-queries.sql > performance-test/results/after-index.txt
```

### 6. 데이터 정리 (테스트 후)
```bash
mysql -h 127.0.0.1 -P 3306 -u application -papplication loopers -e "
TRUNCATE TABLE likes;
TRUNCATE TABLE products;
TRUNCATE TABLE brands;
"
```

## 📊 예상 결과
| 항목 | AS-IS (인덱스 없음) | TO-BE (인덱스 적용) | 개선율 |
|------|-------------------|-------------------|--------|
| 실행 시간 | ~1.2초 | ~0.05초 | **24배** |
| 스캔 방식 | Full Table Scan | Index Range Scan | - |
| 읽은 행 수 | 100,000 | ~1,000 | **100배** |

## ⚠️ 주의사항
1. **로컬 환경 전용**: 이 테스트는 로컬 MySQL에서만 실행하세요.
2. **시간 소요**: 대량 데이터 INSERT는 5-10분 소요될 수 있습니다.
3. **디스크 공간**: 약 500MB~1GB 정도 필요합니다.
4. **기존 데이터**: 테스트 전 기존 데이터를 백업하세요.

## 🔗 참고 문서
- `.claude/round-5-performance.md` - 성능 개선 상세 내역
- `docs/week5/` - 블로그 포스팅용 문서
