# ADR-002: ProductLike는 Hard Delete 전략 사용

## 결정사항
**ProductLike 삭제 시 Hard Delete(물리 삭제)를 사용한다.**

```java
public void unlikeProduct(Long userId, Long productId) {
  // Hard Delete 실행
  productLikeRepository.deleteByUserIdAndProductId(userId, productId);

  // Product likeCount 감소
  product.decreaseLikeCount();
}
```

## 상황
ProductLike 삭제 전략 선택 시 고려사항:
- 좋아요는 선호도 표시이며 법적 보관 의무가 없음
- 대규모 트래픽 환경 가정 (요구사항 2.1.3)
- 사용자가 언제든지 재등록 가능한 데이터
- 조회 성능 최적화 필요

## 선택 근거

### 1. 법적 보관 의무 없음
- 좋아요는 금전 거래 아님
- 세법, 전자상거래법 적용 대상 아님
- Order/Payment와 달리 이력 보관 불필요

### 2. 성능 최적화
**Soft Delete 시 문제점**:
- 모든 조회 쿼리에 `deleted_at IS NULL` 필터 필수
- 삭제된 데이터가 인덱스에 계속 남음
- 테이블 크기 증가 → 조회 성능 저하

**Hard Delete 장점**:
- 테이블 크기 최소화 → 인덱스 효율 증가
- 조회 쿼리가 단순함 (필터링 불필요)

### 3. 비즈니스 의미 일치
- 좋아요 취소 = 데이터 삭제 (직관적)
- 재등록 가능하므로 복구 불필요

### 4. 멱등성 보장
요구사항: "이미 취소한 좋아요 재취소 → 200 OK"
- DELETE 실행 (0건 영향) → 에러 없음
- Product.decreaseLikeCount()에 음수 방어 로직 포함

## 대안: 이력이 필요한 경우

이벤트 기반 아키텍처로 이력 데이터를 분리:
```
ProductLike Delete → Event 발행 → 데이터 웨어하우스 저장
```

**장점**:
- 메인 DB 성능 유지 (Hard Delete)
- 이력은 분석 시스템에서 관리
- 관심사 분리

## 트레이드오프

### ✅ 장점
- 조회 성능 향상 (테이블 크기 최소화)
- 코드 단순성 (필터링 로직 불필요)
- 비즈니스 의미 일치
- 멱등성 보장

### ⚠️ 단점
- 메인 DB에서 즉시 복구 불가
- 이력 추적 제한 (이벤트로 해결 가능)

## 참고
- 요구사항: `docs/design/01-requirements.md` (2.1.3 좋아요 취소 멱등성)
- ERD: `docs/design/04-erd.md`
