---
title: Brand API Sequence Diagram
---
sequenceDiagram
    autonumber
    actor Client
    participant C as BrandController
    participant F as BrandFacade
    participant S as BrandService
    participant R as BrandRepository

    Client->>+C: GET /api/v1/brands/{brandId}
    Note left of C: Brand 상세 조회 API 호출 (brandId)
    Note left of C: Path Variable<br/>--------------------<br/>"brandId":123<br/>---------------------

    C->>+F: getBrandDetail(Long brandId)
    F->>+S: getBrandDetail(Long brandId)

    S->>+R: findById(Long brandId)
    R-->>-S: Optional<Brand>

    rect rgb(20,20,0)
        opt Brand가 존재하지 않는 경우
            S-->>Client: NullPointerException(message)
            Note left of C: "meta": { "result": "FAIL",<br/>"errorCode": NOT_FOUND,<br/>"message": "브랜드를 찾지 못했습니다." },<br/>"data": {}
        end
    end    

    S->>S: Brand -> BrandDetailInfo
    S-->>-F: BrandDetailInfo
    F-->>-C: BrandDetailInfo

    C-->>-Client: 200 OK (BrandDto.DetailResponse JSON 응답)
    Note left of C: "meta": { "result": "SUCCESS", "errorCode": null, "message": null },<br/>"data": {<br/>"id": 1,<br/>"name": "Loopers",<br/>"description": "일상을 더 편하게 만드는 루퍼스",<br/>"status": "ACTIVE",<br/>"isVisible": true,<br/>"isSellable": true<br/>}<br/>