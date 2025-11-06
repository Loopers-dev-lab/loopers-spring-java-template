## 도메인 모델

### 사용자 (User)

#### 속성
- id : Long
- username : 사용자 id (영문 및 숫자 10자 이내)
- email : 이메일 (xx@yy.zz)
- birthDate : 생년월일 (yyyy-mm-dd)

#### 규칙
- username은 고유해야 한다.
- email은 고유해야 한다.
- 속성에 정의된 양식을 준수해서 엔티티를 저장 해야한다.


### 포인트 (Point)

#### 속성
- id : Long
- userId : Long (사용자 ID, User 엔티티와 연관)
- amount : BigDecimal (보유 포인트 금액)
- createdAt : LocalDateTime (생성 시간)
- updatedAt : LocalDateTime (수정 시간)

#### 규칙
- amount는 음수가 될 수 없다.
- userId는 User 엔티티의 id와 연관되어야 한다.
- 포인트는 사용자의 활동에 따라 적립 및 차감될 수 있다.
- 포인트의 이력은 있어야 한다.
- amount는 0 이상이어야 한다.
- amount의 소수점 이하는 2자리까지 허용한다.
- 한 사용자당 하나의 포인트 레코드만 존재해야 한다. (userId는 고유해야 한다)