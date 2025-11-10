# 시퀀스 다이어그램

## 상품 목록 조회
~~~mermaid
sequenceDiagram
    participant C as 클라이언트
    participant AG as API Gateway
    participant PS as Product Service
    participant BS as Brand Service
    participant LS as Like Service

    Note over C,LS: 1. 상품 목록 조회 (GET /api/v1/products)

    C->>AG: GET /api/v1/products?brandId=1&sort=latest&page=0&size=20
    AG->>AG: 요청 파라미터 검증

    alt 유효하지 않은 정렬 키
        AG-->>C: 400 유효하지 않은 정렬 기준입니다.
    else 정렬 키 정상
        AG->>PS: 상품 목록 조회 요청(brandId, sort, page, size)

        alt brandId 파라미터 있음
            PS->>BS: 브랜드 존재 및 활성 상태 확인(brandId)
            alt 브랜드 없음 또는 비활성
                BS-->>PS: 브랜드 없음
                PS-->>AG: 404 해당 브랜드를 찾을 수 없습니다.
                AG-->>C: 404 해당 브랜드를 찾을 수 없습니다.
            else 브랜드 존재
                BS-->>PS: 브랜드 확인 완료
                PS->>PS: 브랜드 조건 포함 조회
            end
        else brandId 파라미터 없음
            PS->>PS: 브랜드 필터 없이 조회 
        end

        alt X-USER-ID 헤더 있음
            PS->>LS: 사용자의 좋아요 정보 조회(상품 ID 목록 기준)
            LS-->>PS: 좋아요 여부 정보
            PS->>PS: 상품 목록에 isLiked 매핑
        end

        PS-->>AG: 상품 목록 결과(상품 정보, totalLikes, isLiked 포함)
        AG-->>C: 200 OK, 상품 목록 응답
    end

    Note over C,LS: 빈 페이지인 경우에도 200 OK와 빈 배열 반환
~~~


## 좋아요 등록
~~~mermaid
sequenceDiagram
    participant C as 클라이언트
    participant AG as API Gateway
    participant LS as Like Service
    participant US as User Service
    participant PS as Product Service

    Note over C,PS: 1. 좋아요 등록 (POST /api/v1/like/products/{productId})<br/>Header: X-USER-ID

    C->>AG: POST /api/v1/like/products/{productId}<br/>Header: X-USER-ID
    AG->>AG: 헤더(X-USER-ID) 검증

    alt X-USER-ID 누락
        AG-->>C: 404 해당 사용자를 찾을 수 없습니다
    else 헤더 OK
        AG->>LS: 좋아요 등록 요청(userId, productId)

        LS->>US: 사용자 존재 확인(userId)
        alt 사용자 없음/유효하지 않음
            US-->>LS: 사용자 없음
            LS-->>AG: 404 해당 사용자를 찾을 수 없습니다
            AG-->>C: 404 해당 사용자를 찾을 수 없습니다
        else 사용자 존재
            US-->>LS: 사용자 확인 완료

            LS->>PS: 상품 존재/활성 확인(productId)
            alt 상품 없음/비활성
                PS-->>LS: 상품 없음/비활성
                LS-->>AG: 404 해당 상품을 찾을 수 없습니다
                AG-->>C: 404 해당 상품을 찾을 수 없습니다
            else 상품 존재
                PS-->>LS: 상품 확인 완료

                LS->>LS: 이미 좋아요 여부 확인(userId, productId)
                alt 이미 좋아요 상태 (멱등)
                    LS-->>AG: 200 OK { liked: true, totalLikes: unchanged }
                    AG-->>C: 200 OK
                else 신규 좋아요
                    LS->>LS: 좋아요 생성
                    LS->>PS: totalLikes 증가 요청(productId)
                    PS-->>LS: 증가 완료
                    LS-->>AG: 200 OK { liked: true, totalLikes: updated }
                    AG-->>C: 200 OK
                end
            end
        end
    end
~~~