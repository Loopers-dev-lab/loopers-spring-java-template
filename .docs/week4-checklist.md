# Week 4 (4주차) 체크리스트

## Must-Have 요구사항

### 1. DB 트랜잭션 적용 ✅
- [x] `@Transactional` 애너테이션을 사용하여 트랜잭션 관리
- [x] OrderFacade.createOrder()에서 트랜잭션 보장
- [x] 실패 시 자동 롤백 동작 확인 (통합 테스트로 검증)
- [x] 쿠폰/재고/포인트 변경의 원자성 보장

### 2. Lock 메커니즘 적용 ✅
- [x] **낙관적 락 (Optimistic Locking)**
  - Product 엔티티에 `@Version` 필드 추가
  - Point 엔티티에 `@Version` 필드 추가
  - UserCoupon 엔티티에 `@Version` 필드 추가

- [x] **비관적 락 (Pessimistic Locking)**
  - ProductRepository.findByIdWithLock() 구현 (`@Lock(PESSIMISTIC_WRITE)`)
  - PointRepository.findByUserIdWithLock() 구현 (`@Lock(PESSIMISTIC_WRITE)`)
  - UserCouponRepository.findByIdWithLock() 구현 (`@Lock(PESSIMISTIC_WRITE)`)
  - OrderFacade에서 모든 공유 자원 조회 시 비관적 락 사용

### 3. 동시성 테스트 작성 ✅
- [x] **테스트 1**: 동일 상품 여러 사용자 좋아요/싫어요 동시 요청
  - ExecutorService와 CountDownLatch 사용
  - 10명의 사용자가 동시에 좋아요 → 모두 성공
  - 동일 사용자들이 동시에 싫어요 → 모두 취소

- [x] **테스트 2**: 동일 쿠폰 여러 기기에서 동시 사용 시도
  - 5개 기기에서 동일 쿠폰으로 주문 시도
  - 비관적 락으로 1개만 성공, 4개 실패 검증

- [x] **테스트 3**: 동일 유저의 서로 다른 주문 동시 수행 (포인트)
  - 잔액 50,000원으로 10,000원 상품 10번 주문
  - 비관적 락으로 5개만 성공, 5개 실패 검증

- [x] **테스트 4**: 동일 상품 여러 주문 동시 요청 (재고)
  - 재고 10개 상품에 20명이 동시 주문
  - 비관적 락으로 10개만 성공, 10개 실패 검증

## Nice-to-Have 요구사항

### 4. 쿠폰 개념 도입 ✅
- [x] **Coupon 엔티티 구현**
  - CouponType (정액/정률 할인)
  - 할인 금액 계산 로직 (calculateDiscountAmount)
  - 정률 할인 시 소수점 버림 처리

- [x] **UserCoupon 엔티티 구현**
  - 사용자별 쿠폰 발급 관리
  - 사용 여부 및 사용 시각 추적
  - 1회 사용 제한 (use() 메서드)
  - Soft delete 지원

- [x] **CouponService 구현**
  - 쿠폰 발급 (issueCoupon)
  - 쿠폰 조회 (getUserCoupon, getUserCouponWithLock)
  - 사용 가능한 쿠폰 목록 조회

- [x] **OrderFacade에 쿠폰 로직 통합**
  - 주문 시 쿠폰 적용 가능
  - 쿠폰 소유자 검증
  - 쿠폰 사용 가능 여부 검증
  - 할인 후 최종 금액 계산

### 5. API 완성도 향상 ✅
- [x] OrderCommand에 쿠폰 ID 추가
- [x] 주문 플로우 완성
  1. 쿠폰 검증 및 사용 처리 (비관적 락)
  2. 상품 재고 확인 및 차감 (비관적 락)
  3. 할인 금액 계산
  4. 포인트 차감 (비관적 락)
  5. 주문 저장

## 테스트 커버리지

### 도메인 단위 테스트 ✅
- [x] **CouponTest** (21개 테스트)
  - 쿠폰 생성 검증 (정액/정률)
  - 할인 금액 계산 검증
  - 유효성 검증 (이름, 타입, 할인값)

- [x] **UserCouponTest** (10개 테스트)
  - 사용자 쿠폰 생성 검증
  - 쿠폰 사용 및 재사용 방지
  - 사용 가능 여부 확인
  - Soft delete 검증

### 동시성 테스트 ✅
- [x] **ConcurrencyTest** (4개 테스트)
  - 좋아요/싫어요 동시 요청
  - 동일 쿠폰 동시 사용
  - 동일 유저 포인트 동시 차감
  - 동일 상품 재고 동시 차감

### 통합 테스트 ✅
- [x] **OrderFacadeIntegrationTest** (8개 테스트)
  - 쿠폰 없이 주문 성공
  - 정액 할인 쿠폰 사용 주문
  - 정률 할인 쿠폰 사용 주문
  - 이미 사용된 쿠폰으로 주문 실패
  - 다른 사용자 쿠폰으로 주문 실패
  - 재고 부족 시 주문 실패 및 롤백
  - 포인트 부족 시 주문 실패 및 롤백
  - 주문 취소 시 재고/포인트 복구

## 문서화 ✅

### UML 다이어그램 업데이트 ✅
- [x] 03-class-diagram.md
  - Coupon 도메인 추가 (섹션 7)
  - UserCoupon 도메인 추가
  - 동시성 제어 전략 문서화

- [x] 04-erd.md
  - coupons 테이블 추가
  - user_coupons 테이블 추가
  - version 필드 설명 추가
  - 락 전략 문서화

### 프로젝트 컨텍스트 ✅
- [x] .claude/project-context.md
  - Week 1-4 요구사항 추적
  - 기술 스택 및 아키텍처 정보
  - Git 브랜칭 전략
  - JPA 베스트 프랙티스

## 구현 세부사항

### 아키텍처 준수 ✅
- [x] Hexagonal Architecture 유지
- [x] 도메인 주도 설계 (DDD) 패턴 적용
- [x] Repository 패턴 (도메인 인터페이스 + JPA 구현체)
- [x] 단방향 참조 (@ManyToOne만 사용, @OneToMany 지양)

### 동시성 제어 전략 ✅
- [x] 읽기 작업: 낙관적 락 (@Version)
- [x] 쓰기 작업: 비관적 락 (@Lock PESSIMISTIC_WRITE)
- [x] 데드락 방지를 위한 락 순서 일관성 유지
- [x] 트랜잭션 격리 수준 관리

### 비즈니스 로직 ✅
- [x] 스냅샷 패턴 (OrderItem에 주문 시점 상품 정보 저장)
- [x] 도메인 엔티티에서 비즈니스 로직 캡슐화
- [x] 불변성 보장 (Builder 패턴, protected 생성자)
- [x] Soft delete 지원 (BaseEntity.deletedAt)

## 테스트 실행 결과 ✅
```
./gradlew :apps:commerce-api:test

BUILD SUCCESSFUL

- 도메인 단위 테스트: 통과
- 동시성 테스트: 통과
- 통합 테스트: 통과
- 전체 테스트: 통과
```

## Git 커밋 이력 ✅
```
e3380ad test: OrderFacade 통합 테스트 추가
ecec664 test: 동시성 제어 통합 테스트 추가
2755121 test: Coupon 및 UserCoupon 도메인 단위 테스트 추가
f301a9d docs: Coupon 도메인 UML 문서 업데이트
f6bdea1 feat: OrderFacade에 쿠폰 적용 로직 및 비관적 락 추가
059df1f feat: Product, Point에 Version 필드 및 비관적 락 추가
2e16cb0 feat: Coupon 도메인 구현
```

## 최종 점검 ✅

### Must-Have 달성률: 100%
- DB 트랜잭션: ✅
- Lock 메커니즘: ✅
- 동시성 테스트: ✅

### Nice-to-Have 달성률: 100%
- 쿠폰 도메인: ✅
- API 완성도: ✅

### 전체 완성도: 100%

## 주요 학습 포인트

1. **트랜잭션 관리**
   - @Transactional을 통한 선언적 트랜잭션
   - 롤백 메커니즘과 원자성 보장
   - 트랜잭션 경계 설정의 중요성

2. **동시성 제어**
   - 낙관적 락 vs 비관적 락의 적절한 사용
   - 데드락 방지 전략
   - 격리 수준과 성능 트레이드오프

3. **테스트 작성**
   - ExecutorService와 CountDownLatch를 활용한 동시성 테스트
   - AtomicInteger로 race condition 회피
   - 통합 테스트에서의 트랜잭션 롤백 검증

4. **도메인 설계**
   - 비즈니스 로직의 도메인 엔티티 캡슐화
   - 불변성과 일관성 유지
   - 명확한 책임 분리 (Facade, Service, Repository)
