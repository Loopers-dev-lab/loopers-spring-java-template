# 📝 Round 3 Quests

---

## 💻 Implementation Quest

> *** 도메인 모델링**을 통해 Product, Brand, Like, Order 기능의 핵심 개념을 **Entity, Value Object, Domain Service 등 적합한** **코드**로 작성합니다.
* 레이어드 아키텍처 + DIP 를 적용해 유연하고 테스트 가능한 구조로 구성합니다.
* **Application Layer를 경량 수준**으로 구성하여, 도메인 객체를 조합하는 흐름을 실제 구현해봅니다.
* **단위 테스트를 작성**하여 도메인 로직의 정합성과 규칙을 검증합니다.
>

### 📋 과제 정보

- 상품, 브랜드, 좋아요, 주문 기능의 **도메인 모델 및 도메인 서비스**를 구현합니다.
- 도메인 간 협력 흐름을 설계하고, 필요한 로직을 **도메인 서비스**로 분리합니다.
- Application Layer에서 도메인 객체를 조합하는 흐름을 구현합니다.
  (예: `ProductFacade.getProductDetail(productId)` → `Product + Brand + Like 조합`)
- Repository Interface 와 구현체는 분리하고, 테스트 가능성을 고려한 구조를 설계합니다.
- 모든 핵심 도메인 로직에 대해 단위 테스트를 작성하고, 예외/경계 케이스도 포함합니다.

### 🏷 Product / Brand 도메인

## ✅ Checklist

- [x]  상품 정보 객체는 브랜드 정보, 좋아요 수를 포함한다.
- [x]  상품의 정렬 조건(`latest`, `price_asc`, `likes_desc`) 을 고려한 조회 기능을 설계했다
- [x]  상품은 재고를 가지고 있고, 주문 시 차감할 수 있어야 한다
- [x]  재고는 감소만 가능하며 음수 방지는 도메인 레벨에서 처리된다

### 👍 Like 도메인

- [x]  좋아요는 유저와 상품 간의 관계로 별도 도메인으로 분리했다
- [x]  중복 좋아요 방지를 위한 멱등성 처리가 구현되었다
- [x]  상품의 좋아요 수는 상품 상세/목록 조회에서 함께 제공된다
- [x]  단위 테스트에서 좋아요 등록/취소/중복 방지 흐름을 검증했다

### 🛒 Order 도메인

- [x]  주문은 여러 상품을 포함할 수 있으며, 각 상품의 수량을 명시한다
- [x]  주문 시 상품의 재고 차감, 유저 포인트 차감 등을 수행한다
- [x]  재고 부족, 포인트 부족 등 예외 흐름을 고려해 설계되었다
- [x]  단위 테스트에서 정상 주문 / 예외 주문 흐름을 모두 검증했다

### 🧩 도메인 서비스

- [x]  도메인 간 협력 로직은 Domain Service에 위치시켰다
- [x]  상품 상세 조회 시 Product + Brand 정보 조합은 도메인 서비스에서 처리했다
- [x]  복합 유스케이스는 Application Layer에 존재하고, 도메인 로직은 위임되었다
- [x]  도메인 서비스는 상태 없이, 도메인 객체의 협력 중심으로 설계되었다

### **🧱 소프트웨어 아키텍처 & 설계**

- [x]  전체 프로젝트의 구성은 아래 아키텍처를 기반으로 구성되었다
    - Application → **Domain** ← Infrastructure
- [x]  Application Layer는 도메인 객체를 조합해 흐름을 orchestration 했다
- [x]  핵심 비즈니스 로직은 Entity, VO, Domain Service 에 위치한다
- [x]  Repository Interface는 Domain Layer 에 정의되고, 구현체는 Infra에 위치한다
- [x]  패키지는 계층 + 도메인 기준으로 구성되었다 (`/domain/order`, `/application/like` 등)
- [x]  테스트는 외부 의존성을 분리하고, Fake/Stub 등을 사용해 단위 테스트가 가능하게 구성되었다