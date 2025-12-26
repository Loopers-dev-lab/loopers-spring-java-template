package com.loopers.domain.ranking;

import java.util.List;

public interface RankingRepository {

  List<RankingEntry> getTopN(String key, int page, int size);

  Integer getRank(String key, Long productId);
}
