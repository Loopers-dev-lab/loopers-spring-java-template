# Tests

테스트 관련 도구 및 스크립트를 모아둔 폴더입니다.

## 구조

```
tests/
├── http/              # HTTP 클라이언트 테스트 파일
│   ├── commerce-api/
│   └── pg-simulator/
└── k6/                # k6 부하 테스트 스크립트
    ├── scripts/
    ├── utils/
    ├── config/
    ├── data/
    └── results/
```

## HTTP 테스트

HTTP 클라이언트를 사용한 수동 API 테스트 파일입니다.

- `http/commerce-api/` - Commerce API 테스트
- `http/pg-simulator/` - PG Simulator 테스트

## k6 부하 테스트

k6를 사용한 자동화된 부하 테스트 스크립트입니다.

자세한 내용은 [k6/README.md](./k6/README.md)를 참고하세요.

### 빠른 시작

```bash
# k6 설치 (macOS)
brew install k6

# 기본 부하 테스트 실행
cd tests/k6
k6 run scripts/commerce-api/order-create.js
```

