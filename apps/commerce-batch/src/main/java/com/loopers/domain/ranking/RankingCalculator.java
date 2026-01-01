package com.loopers.domain.ranking;

import org.springframework.stereotype.Component;

@Component
public class RankingCalculator {

    public double calculateScore(Long likeCount, Long viewCount, Long salesCount) {
        double score = 0.0;
        
        if (viewCount != null && viewCount > 0) {
            score += viewCount * RankingWeight.VIEW.getWeight();
        }
        
        if (likeCount != null && likeCount > 0) {
            score += likeCount * RankingWeight.LIKE.getWeight();
        }
        
        if (salesCount != null && salesCount > 0) {
            score += salesCount * RankingWeight.ORDER_CREATED.getWeight();
        }
        
        return score;
    }
}


