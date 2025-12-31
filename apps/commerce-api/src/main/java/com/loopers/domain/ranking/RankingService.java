package com.loopers.domain.ranking;

import com.loopers.ranking.streamer.RankingInfo;
import com.loopers.ranking.streamer.RankingReadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RankingService {
    private final RankingReadRepository rankingReadRepository;


    public List<RankingInfo> getRankings(LocalDate rankingDate, Pageable pageable) {
        return rankingReadRepository.findPage(rankingDate, pageable.getPageNumber(), pageable.getPageSize());
    }

    public long getTotalCount(LocalDate rankingDate) {
        return rankingReadRepository.findTotalCount(rankingDate);
    }

    public Long getRanking(LocalDate rankingDate, long productId) {
        return rankingReadRepository.findRank(rankingDate, productId);
    }
}
