## 1. 상품 목록 조회
```mermaid
sequenceDiagram
    actor a as Actor
    participant Controller as ProductController
    participant Facade as ProductFacade
    participant Product as Product
    participant Brand as Brand
    participant User as User
    participant Like as Like

    a ->> Controller: GET /api/v1/products<br/>(정렬, 필터링, 페이지 정보)
    Controller ->> Facade: 상품 목록 조회 요청<br/>(정렬, 필터링, 페이지 정보, 로그인 ID)
    
    Facade ->> Brand: 브랜드 필터링 조건 검증
    Brand -->> Facade: 검증 결과 반환
    
    Facade ->> Product: 조건에 맞는 상품 목록 조회
    Product -->> Facade: 상품 목록 반환

    Facade ->> Product: 각 상품의 좋아요 수 조회
    Product -->> Facade: 좋아요 수 정보 반환

    opt 로그인한 사용자
        Facade ->> User: 사용자 인증 정보 확인
        User -->> Facade: 사용자 정보 반환
        
        Facade ->> Like: 사용자의 좋아요 여부 조회
        Like -->> Facade: 좋아요 여부 목록 반환
    end

    Facade -->> Controller: 상품 목록 + 좋아요 수 + (좋아요 여부) 반환
    Controller -->> a: 200 OK<br/>(브랜드명, 상품명, 가격, 좋아요 수, 좋아요 여부)
```

---

## 2. 상품 상세 조회
```mermaid
sequenceDiagram
    actor a as Actor
    participant Controller as ProductController
    participant Facade as ProductFacade
    participant Product as Product
    participant Brand as Brand
    participant User as User
    participant Like as Like

    a ->> Controller: GET /api/v1/products/{productId}
    Controller ->> Facade: 상품 상세 조회 요청<br/>(productId, 로그인 ID)
    
    Facade ->> Product: 상품 존재 여부 및 상세 정보 조회
    alt 상품 존재하지 않음
        Product -->> Facade: Not Found
        Facade -->> Controller: 404 Not Found
        Controller -->> a: 404 Not Found (존재하지 않는 상품)
    else 상품 존재함
        Product -->> Facade: 상품 상세 정보 반환<br/>(상품명, 가격, 재고, 등록일, 브랜드 ID)
    end

    Facade ->> Brand: 브랜드명 조회<br/>(브랜드 ID)
    Brand -->> Facade: 브랜드명 반환

    Facade ->> Product: 해당 상품의 좋아요 수 조회
    Product -->> Facade: 좋아요 수 반환

    opt 로그인한 사용자
        Facade ->> User: 사용자 인증 정보 확인
        User -->> Facade: 사용자 정보 반환

        Facade ->> Like: 해당 상품에 대한 사용자의 좋아요 여부 조회
        Like -->> Facade: 좋아요 여부 반환
    end

    Facade -->> Controller: 상품 상세 정보 + 브랜드명 + 좋아요 수 + (좋아요 여부)
    Controller -->> a: 200 OK<br/>(상품명, 브랜드명, 가격, 재고, 좋아요 수, 좋아요 여부, 등록일)
```

---

## 3. 브랜드 정보 조회
```mermaid
sequenceDiagram
    actor a as Actor
    participant Controller as BrandController
    participant Facade as BrandFacade
    participant Brand as Brand

    a ->> Controller: GET /api/v1/brands/{brandId}
    Controller ->> Facade: 브랜드 상세 조회 요청<br/>(brandId)

    Facade ->> Brand: 브랜드 존재 여부 및 상세 정보 조회
    alt 브랜드 존재하지 않음
        Brand -->> Facade: Not Found
        Facade -->> Controller: 404 Not Found
        Controller -->> a: 404 Not Found (존재하지 않는 브랜드)
    else 브랜드 존재함
        Brand -->> Facade: 브랜드 상세 정보 반환<br/>(브랜드명, 브랜드 소개)
        Facade -->> Controller: 브랜드 상세 정보 반환
        Controller -->> a: 200 OK<br/>(브랜드명, 브랜드 소개)
    end
```

---

## 4. 상품 좋아요 등록
```mermaid
sequenceDiagram
    actor a as Actor
    participant Controller as LikeController
    participant Facade as LikeFacade
    participant User as User
    participant Product as Product
    participant Like as Like

    a ->> Controller: POST /api/v1/like/products/{productId}
    Controller ->> User: 사용자 인증 정보 확인
    alt 인증 실패
        User -->> Controller: Unauthorized
        Controller -->> a: 401 Unauthorized (로그인 필요)
    else 인증 성공
        Controller ->> Facade: 상품 좋아요 등록 요청<br/>(userId, productId)

        Facade ->> Product: 상품 존재 여부 확인
        alt 상품 없음
            Product -->> Facade: Not Found
            Facade -->> Controller: 404 Not Found
            Controller -->> a: 404 Not Found (존재하지 않는 상품)
        else 상품 존재
            Facade ->> Like: 사용자 기존 좋아요 여부 확인
            alt 이미 좋아요한 상품
                Like -->> Facade: Already Liked
                Facade -->> Controller: 400 Bad Request (중복 좋아요 불가)
                Controller -->> a: 400 Bad Request
            else 좋아요하지 않은 상품
                Like -->> Facade: Not Liked
                Facade ->> Like: 좋아요 등록 및 등록 일시 저장
                Facade ->> Product: 좋아요 수 +1 업데이트
                Facade -->> Controller: 성공 응답<br/>(좋아요됨, 현재 좋아요 수)
                Controller -->> a: 201 Created<br/>(좋아요됨, 좋아요 수)
            end
        end
    end
```

---

## 5. 상품 좋아요 취소
```mermaid
sequenceDiagram
    actor a as Actor
    participant Controller as LikeController
    participant Facade as LikeFacade
    participant User as User
    participant Product as Product
    participant Like as Like

    a ->> Controller: DELETE /api/v1/products/{productId}/likes
    Controller ->> User: 사용자 인증 정보 확인
    alt 인증 실패
        User -->> Controller: Unauthorized
        Controller -->> a: 401 Unauthorized (로그인 필요)
    else 인증 성공
        Controller ->> Facade: 상품 좋아요 취소 요청<br/>(userId, productId)

        Facade ->> Product: 상품 존재 여부 확인
        alt 상품 없음
            Product -->> Facade: Not Found
            Facade -->> Controller: 404 Not Found
            Controller -->> a: 404 Not Found (존재하지 않는 상품)
        else 상품 존재
            Facade ->> Like: 사용자의 좋아요 여부 확인
            alt 좋아요하지 않은 상품
                Like -->> Facade: Not Liked
                Facade -->> Controller: 400 Bad Request (좋아요하지 않은 상품)
                Controller -->> a: 400 Bad Request
            else 좋아요한 상품
                Like -->> Facade: Liked
                Facade ->> Like: 좋아요 내역 삭제
                Facade ->> Product: 좋아요 수 -1 업데이트
                Facade -->> Controller: 성공 응답<br/>(좋아요 취소됨, 현재 좋아요 수)
                Controller -->> a: 200 OK<br/>(좋아요 취소됨, 좋아요 수)
            end
        end
    end
```

---

## 6. 좋아요한 상품 목록 조회
```mermaid
sequenceDiagram
    actor a as Actor
    participant Controller as LikeController
    participant Facade as LikeFacade
    participant User as User
    participant Like as Like
    participant Product as Product
    participant Brand as Brand

    a ->> Controller: GET /api/v1/like/products<br/>(페이지 번호, 페이지 크기)
    Controller ->> User: 사용자 인증 정보 확인
    alt 인증 실패
        User -->> Controller: Unauthorized
        Controller -->> a: 401 Unauthorized (로그인 필요)
    else 인증 성공
        Controller ->> Facade: 좋아요한 상품 목록 조회 요청<br/>(userId, 페이지 정보)

        Facade ->> Like: 사용자의 좋아요한 상품 목록 조회<br/>(좋아요 등록일시 기준 내림차순, 페이지네이션)
        alt 좋아요한 상품 없음
            Like -->> Facade: 빈 목록 반환
            Facade -->> Controller: 빈 목록 반환
            Controller -->> a: 200 OK<br/>(빈 배열)
        else 좋아요한 상품 존재
            Like -->> Facade: 상품 ID 목록 + 좋아요 등록일시 반환

            Facade ->> Product: 상품 ID 목록으로 상품 정보 조회<br/>(상품명, 가격, 브랜드 ID)
            Product -->> Facade: 상품 기본 정보 목록 반환

            Facade ->> Brand: 브랜드명 조회<br/>(각 상품의 브랜드 ID)
            Brand -->> Facade: 브랜드명 목록 반환

            Facade -->> Controller: 좋아요한 상품 목록 반환<br/>(브랜드명, 상품명, 가격, 좋아요 등록일시)
            Controller -->> a: 200 OK<br/>(좋아요한 상품 목록, 페이지 정보)
        end
    end
```

---

## 7. 주문 요청 
```mermaid
sequenceDiagram
    actor a as Actor
    participant Controller as OrderController
    participant Facade as OrderFacade
    participant User as User
    participant Product as Product
    participant Point as Point
    participant Order as Order
    participant Payment as Payment
    participant Delivery as Delivery

    a ->> Controller: POST /api/v1/orders<br/>(주문 상품 목록[{productId, quantity}], 배송 정보)
    Controller ->> User: 사용자 인증 정보 확인
    alt 인증 실패
        User -->> Controller: Unauthorized
        Controller -->> a: 401 Unauthorized (로그인 필요)
    else 인증 성공
        Controller ->> Facade: 주문 생성 및 결제 요청<br/>(userId, 주문 상품 목록, 배송 정보)

        loop 각 상품에 대해
            Facade ->> Product: 상품 존재 여부 및 재고 조회
            alt 상품 없음
                Product -->> Facade: Not Found
                Facade -->> Controller: 404 Not Found (존재하지 않는 상품)
                Controller -->> a: 404 Not Found
            else 상품 존재
                Product -->> Facade: 상품 정보 반환 (가격, 재고)
                alt 재고 부족
                    Facade -->> Controller: 400 Bad Request (재고 부족)
                    Controller -->> a: 400 Bad Request
                else 재고 충분
                end
            end
        end

        Facade ->> Point: 사용자 포인트 조회
        Point -->> Facade: 포인트 잔액 반환

        Facade ->> Product: 총 주문 금액 계산
        alt 포인트 부족
            Facade -->> Controller: 400 Bad Request (포인트 부족)
            Controller -->> a: 400 Bad Request
        else 결제 가능
            loop 각 상품에 대해
                Facade ->> Product: 재고 차감
                Product -->> Facade: 재고 차감 완료
            end

            Facade ->> Point: 포인트 차감 (총 주문 금액만큼)
            Point -->> Facade: 포인트 차감 완료

            Facade ->> Delivery: 배송 정보 생성<br/>(수령인, 주소)
            Delivery -->> Facade: 배송 정보 객체 반환

            Facade ->> Order: 주문 정보 저장<br/>(주문자, 상품 목록, 총 금액, 주문 상태, 배송 정보)
            Order -->> Facade: 주문 저장 완료

            Facade ->> Payment: 결제 정보 생성 및 저장<br/>(주문, 결제 금액, 결제 상태)
            Payment -->> Facade: 결제 완료 정보 반환

            Facade -->> Controller: 주문 완료 정보 반환<br/>(주문 ID, 주문 번호, 상품 목록, 총 금액, 배송 정보, 결제 상태, 결제 일시)
            Controller -->> a: 201 Created<br/>(주문 완료 응답)
        end
    end
```

---

## 8. 유저 주문 목록 조회
```mermaid
sequenceDiagram
    actor a as Actor
    participant Controller as OrderController
    participant Facade as OrderFacade
    participant User as User
    participant Order as Order

    a ->> Controller: GET /api/v1/orders<br/>(페이지 번호, 페이지 크기)
    Controller ->> User: 사용자 인증 정보 확인
    alt 인증 실패
        User -->> Controller: Unauthorized
        Controller -->> a: 401 Unauthorized (로그인 필요)
    else 인증 성공
        Controller ->> Facade: 사용자 주문 목록 조회 요청<br/>(userId, 페이지 정보)

        Facade ->> Order: 사용자 주문 목록 조회<br/>(최신 주문일 기준 내림차순, 페이지네이션)
        alt 주문 없음
            Order -->> Facade: 빈 목록 반환
            Facade -->> Controller: 빈 목록 반환
            Controller -->> a: 200 OK<br/>(빈 배열)
        else 주문 존재
            Order -->> Facade: 주문 목록 반환<br/>(주문 번호, 주문 날짜, 총 결제 금액, 주문 상태)
            Facade -->> Controller: 주문 목록 반환
            Controller -->> a: 200 OK<br/>(주문 목록, 페이지 정보)
        end
    end
```

---

## 9. 단일 주문 상세 조회
```mermaid
sequenceDiagram
    actor a as Actor
    participant Controller as OrderController
    participant Facade as OrderFacade
    participant User as User
    participant Order as Order
    participant Product as Product
    participant Brand as Brand
    participant Delivery as Delivery
    participant Receiver as Receiver
    participant Address as Address

    a ->> Controller: GET /api/v1/orders/{orderId}
    Controller ->> User: 사용자 인증 정보 확인
    alt 인증 실패
        User -->> Controller: Unauthorized
        Controller -->> a: 401 Unauthorized (로그인 필요)
    else 인증 성공
        Controller ->> Facade: 단일 주문 상세 조회 요청<br/>(userId, orderId)

        Facade ->> Order: 주문 존재 여부 및 상세 정보 조회
        alt 주문 없음
            Order -->> Facade: Not Found
            Facade -->> Controller: 404 Not Found (존재하지 않는 주문)
            Controller -->> a: 404 Not Found
        else 주문 존재
            Order -->> Facade: 주문 상세 정보 반환<br/>(주문자 ID, 주문 날짜, 총 결제 금액, 주문 상태, 주문 상품 목록, 배송 정보)

            alt 다른 사용자 주문
                Facade -->> Controller: 403 Forbidden (접근 권한 없음)
                Controller -->> a: 403 Forbidden
            else 본인 주문
                loop 각 주문 상품에 대해
                    Facade ->> Product: 상품 정보 조회<br/>(상품명, 가격, 브랜드 ID)
                    alt 상품 삭제됨
                        Product -->> Facade: Deleted Product Info<br/>(삭제된 상품 표시)
                    else 상품 존재
                        Product -->> Facade: 상품 정보 반환
                        Facade ->> Brand: 브랜드명 조회<br/>(브랜드 ID)
                        Brand -->> Facade: 브랜드명 반환
                    end
                end

                Facade ->> Delivery: 배송 정보 조회
                Delivery -->> Facade: 수령인, 주소 정보 반환
                Facade ->> Receiver: 수령인 정보 조회
                Receiver -->> Facade: 이름, 전화번호 반환
                Facade ->> Address: 주소 정보 조회
                Address -->> Facade: 기본주소, 상세주소 반환

                Facade -->> Controller: 주문 상세 정보 반환<br/>(주문 정보 + 상품 정보 + 배송 정보)
                Controller -->> a: 200 OK<br/>(주문 번호, 날짜, 총 금액, 상태, 상품 목록, 배송 정보)
            end
        end
    end
```
