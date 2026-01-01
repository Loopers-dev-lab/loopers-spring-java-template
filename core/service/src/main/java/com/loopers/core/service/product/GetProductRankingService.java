package com.loopers.core.service.product;

import com.loopers.core.domain.product.ProductRankingList;
import com.loopers.core.domain.product.repository.ProductRankingCacheRepository;
import com.loopers.core.domain.product.repository.ProductRepository;
import com.loopers.core.domain.product.vo.ProductRankings;
import com.loopers.core.service.product.query.GetProductRankingQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GetProductRankingService {

    private final ProductRankingCacheRepository productRankingCacheRepository;
    private final ProductRepository productRepository;

    public ProductRankingList getRanking(GetProductRankingQuery query) {
        ProductRankings rankings = productRankingCacheRepository.getRankings(query.date(), query.pageNo(), query.pageSize());
        ProductRankingList rankingList = productRepository.findRankingListBy(rankings.getProducts());

        return rankingList.with(rankings);
    }
}
