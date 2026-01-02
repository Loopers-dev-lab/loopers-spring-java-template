# 📝 Round 10 Quests

---

## 💻 Implementation Quest

> 이번에는 Spring Batch 를 활용해 주간, 월간 랭킹을 제공해 볼 거예요.
이전에 적재했던 `product_metrics` 와 같은 일간 집계정보를 기반으로 **주간, 월간 랭킹 시스템을 구축**해봅니다.
>

<aside>
🎯

**Must-Have (이번 주에 무조건 가져가야 좋을 것-**무조건하세요**)**

- Spring Batch
- Batch Processing
- Materialized View (Statistics)
</aside>

### 📋 과제 정보

이번 주는 대규모 데이터 집계 및 조회 전용 구조에 대한 설계를 진행해 봅니다.

### (1) Spring Batch Job 구현

- 하루치 메트릭 테이블을 읽어 데이터를 집계하고 처리해봅니다.
    - 대상 테이블 : `product_metrics`
    - Chunk-Oriented 방식을 통해 대량의 데이터를 읽고 처리할 수 있도록 구성해 보세요.

### (2) Materialized View 설계

- 집계 결과를 조회 전용 테이블 (MV) 로 저장합니다.
    - `mv_product_rank_weekly` : 주간 TOP 100 랭킹
    - `mv_product_rank_monthly` : 월간 TOP 100 랭킹

### (3) Ranking API 확장

- 기존 Ranking 을 제공하는 GET `/api/v1/rankings?date=yyyyMMdd&size=20&page=1` 에서 기간 정보를 전달받아 API 로 일간, 주간, 월간 랭킹을 제공할 수 있도록 개선합니다.

---

## ✅ Checklist

### 🧱 Spring Batch

- [x]  Spring Batch Job 을 작성하고, 파라미터 기반으로 동작시킬 수 있다.
- [x]  Chunk Oriented Processing (Reader/Processor/Writer or Tasklet) 기반의 배치 처리를 구현했다.
- [x]  집계 결과를 저장할 Materialized View 의 구조를 설계하고 올바르게 적재했다.

### 🧩 Ranking API

- [x]  API 가 일간, 주간, 월간 랭킹을 제공하며 조회해야 하는 형태에 따라 적절한 데이터를 기반으로 랭킹을 제공한다.
