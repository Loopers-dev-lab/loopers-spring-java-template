package com.loopers.domain.ranking;

import java.time.LocalDate;

public interface RankingKeyPolicy {
  String buildKey(LocalDate date);
}