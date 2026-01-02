package com.loopers.domain.rank;

import org.springframework.stereotype.Component;

public interface RankingKeyGenerator {

  String generateDailyKey(String date);
}

