# Sequence Diagrams

이 문서는 시스템의 주요 플로우를 나타내는 시퀀스 다이어그램들을 정리합니다.

## 1. Order and Payment Flow
주문 및 결제 처리 플로우 (3단계 분리: 주문 생성 → 결제 요청 → 콜백 처리)

[시퀀스 다이어그램 소스](sequence/OrderAndPaymentFlow.puml)

### 플로우 설명
1. **주문 생성**: 클라이언트가 주문을 생성하면 상품 유효성 검증 후 `PENDING_PAYMENT` 상태로 주문 엔티티 생성
2. **결제 요청**: 주문번호로 결제를 요청하면 PG사 결제 URL/token 반환
3. **결제 완료**: PG사에서 콜백으로 결제 완료 통지 시 재고 차감, 포인트 차감, 주문 상태를 `PAYMENT_COMPLETED`로 변경

## 2. Payment Callback
결제 콜백 처리 (성공/실패 케이스 포함)

[시퀀스 다이어그램 소스](sequence/PaymentCallBack.puml)

### 플로우 설명
- PG사에서 결제 결과를 콜백으로 전송
- 주문번호, 금액 유효성 검증 후 결제 성공/실패에 따라 처리
- **성공 시**: 재고 차감, 포인트 차감, 결제/주문 상태 업데이트
- **실패 시**: 결제/주문 상태를 실패로 변경
- 중복 콜백 처리 방지 (멱등성 보장)

## 3. Product List
상품 목록 조회 (정렬, 페이징 지원)

[시퀀스 다이어그램 소스](sequence/getProductList.puml)

### 플로우 설명
- 인기순 정렬을 기본으로 상품 목록 조회
- 페이징 처리 (`page`, `size` 파라미터)
- 각 상품에 브랜드 정보 포함
- 응답: 상품 ID, 이름, 가격, 브랜드명, 썸네일, 좋아요 수, 재고 상태

## 4. Like
좋아요 기능 (상품/브랜드 토글, 멱등성 보장)

[시퀀스 다이어그램 소스](sequence/like.puml)

### 플로우 설명
- **상품 좋아요**: `/api/v1/products/{productId}/likes` POST
- **브랜드 좋아요**: `/api/v1/brands/{brandId}/likes` POST
- 토글 방식: 이미 좋아요한 경우 취소, 없으면 등록
- 좋아요 수 증가/감소 처리
- 멱등성 보장 (동일 요청 반복 시 상태 유지)

## 5. Brand List
브랜드 목록 조회 (정렬, 필터링 지원)

[시퀀스 다이어그램 소스](sequence/getBrandList.puml)

### 플로우 설명
- 가나다순 정렬 지원 (`sortBy=name`)
- 좋아요한 브랜드만 필터링 (`likedOnly=true`)
- 선택적 인증 (로그인 시 좋아요 여부 포함)
- 응답: 브랜드 ID, 이름, 설명, SNS 링크, 좋아요 수, 관련 상품 수

## 6. Order List
주문 목록 조회 (사용자별)

[시퀀스 다이어그램 소스](sequence/getOrderList.puml)

### 플로우 설명
- 인증된 사용자의 주문 목록을 최신순으로 조회
- 각 주문의 OrderItem 정보와 상품 썸네일 포함
- 응답: 주문번호, 주문시각, 상태, 결제금액, 주문항목 수, 대표 상품 썸네일

## 7. Cancel Order
주문 취소 (리소스 복구 포함)

[시퀀스 다이어그램 소스](sequence/cancelOrder.puml)

### 플로우 설명
- `PENDING_PAYMENT` 상태의 주문만 취소 가능
- 주문 권한 확인 (본인 주문인지 검증)
- 취소 시 리소스 복구: 재고 복구, 포인트 복구
- 주문 상태를 `CANCELLED`로 변경

---

## 공통 설계 원칙

### 1. 인증/권한 처리
- 모든 API는 `X-USER-ID` 헤더로 사용자 인증
- 일부 조회 API는 선택적 인증 (비로그인 시에도 기본 정보 제공)

### 2. 멱등성 보장
- 좋아요 토글: 중복 요청 시 상태 유지
- 결제 콜백: 중복 처리 방지
- 주문 취소: 이미 취소된 주문 재처리 방지

### 3. 데이터 일관성
- 결제 완료 시점에 재고/포인트 차감
- 주문 취소 시 리소스 복구
- 트랜잭션 처리로 데이터 무결성 보장

### 4. 상태 관리
- 주문: `PENDING_PAYMENT` → `PAYMENT_COMPLETED` / `PAYMENT_FAILED` / `CANCELLED`
- 결제: `INITIATED` → `PROCESSING` → `COMPLETED` / `FAILED`