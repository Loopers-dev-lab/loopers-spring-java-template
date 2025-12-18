# Kafka 설정 학습 테스트

## 📚 목적

Kafka 설정들을 실제 코드로 테스트하면서 동작 방식을 학습합니다.

**주의:** 이 테스트들은 프로덕션 코드와 무관한 **학습용 테스트**입니다.

---

## 🛠️ 사전 준비

### ✨ Docker 불필요!

이 학습 테스트는 **Embedded Kafka**를 사용합니다:
- ✅ 테스트 실행 시 자동으로 Kafka 시작
- ✅ 테스트 종료 시 자동으로 정리
- ✅ Docker 설치 불필요
- ✅ 토픽 자동 생성

**바로 IntelliJ에서 테스트를 실행하면 됩니다!**

### (선택) Docker Kafka 사용하려면

만약 실제 Docker Kafka로 테스트하고 싶다면:

```bash
# Docker Compose로 Kafka 실행
docker-compose up -d

# @EmbeddedKafka의 brokerProperties를 제거하고
# BOOTSTRAP_SERVERS를 "localhost:19092"로 변경
```

---

## 🧪 학습 테스트 목록

### 1. Auto Commit vs Manual Commit
**파일:** `Experiment1_AutoCommitTest.java`

**학습 내용:**
- Auto Commit의 메시지 유실 가능성
- Manual Commit의 안전성
- 커밋 타이밍의 중요성

**실행:**
```bash
./gradlew :modules:kafka:test --tests "*Experiment1*"
```

---

### 2. auto.offset.reset
**파일:** `Experiment2_OffsetResetTest.java`

**학습 내용:**
- earliest: 과거 메시지부터 읽기
- latest: 새 메시지만 읽기
- 기존 오프셋이 있을 때 동작

**실행:**
```bash
./gradlew :modules:kafka:test --tests "*Experiment2*"
```

---

### 3. max.poll.interval.ms
**파일:** `Experiment3_PollIntervalTest.java`

**학습 내용:**
- 타임아웃 발생 시 리밸런싱
- 처리 시간과 타임아웃의 관계
- 배치 크기로 타임아웃 방지

**주의:** 일부 테스트는 시간이 오래 걸립니다 (2분)

**실행:**
```bash
./gradlew :modules:kafka:test --tests "*Experiment3*"
```

---

### 4. max.poll.records
**파일:** `Experiment4_BatchSizeTest.java`

**학습 내용:**
- 배치 크기에 따른 처리량 차이
- 큰 배치 vs 작은 배치 트레이드오프
- 최적 배치 크기 찾기

**실행:**
```bash
./gradlew :modules:kafka:test --tests "*Experiment4*"
```

---

## 📖 실행 방법

### 방법 1: IntelliJ에서 직접 실행 (추천)

1. 테스트 메서드 왼쪽의 ▶️ 버튼 클릭
2. 또는 메서드 내에서 `Ctrl + Shift + R` (Mac: `Cmd + Shift + R`)
3. 또는 클래스 전체 실행: 클래스명 옆 ▶️ 클릭

**장점:**
- `@Disabled` 제거했으므로 바로 실행 가능
- 로그를 실시간으로 확인 가능
- 각 테스트를 독립적으로 실행

### 방법 2: Gradle 명령어

```bash
# 전체 학습 테스트 실행
./gradlew :modules:kafka:test --tests "com.loopers.kafka.learning.*"

# 특정 실험만 실행
./gradlew :modules:kafka:test --tests "*Experiment1*"
./gradlew :modules:kafka:test --tests "*Experiment2*"

# 특정 메서드만 실행
./gradlew :modules:kafka:test --tests "*Experiment1*.autoCommit_MessageLoss"
```

---

## 🔍 로그 확인 팁

실험 실행 시 로그를 주의깊게 봐야 합니다:

```
=== 1단계: 메시지 10개 전송 ===
메시지 전송 완료: 10개

=== 2단계: Consumer 시작 ===
읽은 메시지 개수: 10

=== 3단계: 재시작 ===
읽은 메시지 개수: 0 (이미 커밋됨)
```

---

## 💡 학습 팁

### 1. 순서대로 진행

1. **Experiment2** (offset.reset) - 가장 간단
2. **Experiment4** (batch size) - 효과가 명확
3. **Experiment1** (auto commit) - 중요한 개념
4. **Experiment3** (poll interval) - 시간 오래 걸림

### 2. 설정 값을 바꿔보기

```java
// 타임아웃을 더 짧게
props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, "10000");

// 배치 크기를 바꿔보기
props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, "50");
```

### 3. Consumer Group 관리

```bash
# Consumer Group 목록
kafka-consumer-groups.sh --bootstrap-server localhost:19092 --list

# 오프셋 확인
kafka-consumer-groups.sh --bootstrap-server localhost:19092 \
  --group learning-group \
  --describe

# Group 삭제 (재실험 시)
kafka-consumer-groups.sh --bootstrap-server localhost:19092 \
  --group learning-group \
  --delete
```

---

## 🛠️ 트러블슈팅

### Consumer가 메시지를 못 읽어요

**확인 사항:**
1. Kafka 실행 중? `docker ps | grep kafka`
2. 토픽 생성됨? `kafka-topics.sh --list`
3. `auto.offset.reset` 설정 확인

**해결:**
```bash
# Consumer Group 리셋
kafka-consumer-groups.sh --bootstrap-server localhost:19092 \
  --group 그룹명 --reset-offsets --to-earliest --topic learning-topic --execute
```

### 테스트가 멈춰요

**원인:** Kafka 연결 실패

**해결:**
```bash
docker-compose restart kafka
```

---

## 📚 참고 문서

- `.claude/round-8/kafka-configuration-guide.md` - 상세 설정 가이드
- `.claude/round-8/exactly-once-semantics.md` - Exactly-Once 개념
- `.claude/round-8/inbox-pattern-analysis.md` - Inbox 패턴 분석
