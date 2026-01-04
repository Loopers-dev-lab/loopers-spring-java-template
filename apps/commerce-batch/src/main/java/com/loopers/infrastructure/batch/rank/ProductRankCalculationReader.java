package com.loopers.infrastructure.batch.rank;

import com.loopers.domain.rank.ProductRankScore;
import com.loopers.domain.rank.ProductRankScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.stereotype.Component;

import java.util.Iterator;
import java.util.List;

/**
 * ProductRankScore를 읽는 Reader.
 * <p>
 * Step 2 (랭킹 로직 실행 Step)에서 사용합니다.
 * ProductRankScore 테이블에서 점수 내림차순으로 모든 데이터를 읽습니다.
 * </p>
 * <p>
 * <b>구현 의도:</b>
 * <ul>
 *   <li>Step 1에서 집계된 모든 ProductRankScore를 읽기</li>
 *   <li>점수 내림차순으로 정렬된 데이터를 제공</li>
 *   <li>TOP 100 선정을 위해 전체 데이터를 읽어야 함</li>
 * </ul>
 * </p>
 *
 * @author Loopers
 * @version 1.0
 */
@Slf4j
@Component
@StepScope
@RequiredArgsConstructor
public class ProductRankCalculationReader implements ItemReader<ProductRankScore> {

    private final ProductRankScoreRepository productRankScoreRepository;
    private Iterator<ProductRankScore> scoreIterator;
    private boolean initialized = false;

    /**
     * ProductRankScore를 읽습니다.
     * <p>
     * 첫 호출 시 모든 데이터를 조회하고, 이후 Iterator를 통해 하나씩 반환합니다.
     * </p>
     *
     * @return ProductRankScore (더 이상 없으면 null)
     * @throws UnexpectedInputException 예상치 못한 입력 오류
     * @throws ParseException 파싱 오류
     * @throws NonTransientResourceException 일시적이지 않은 리소스 오류
     */
    @Override
    public ProductRankScore read() throws Exception, UnexpectedInputException, ParseException, NonTransientResourceException {
        if (!initialized) {
            // 첫 호출 시 모든 데이터를 점수 내림차순으로 조회
            List<ProductRankScore> scores = productRankScoreRepository.findAllOrderByScoreDesc(0);
            this.scoreIterator = scores.iterator();
            this.initialized = true;
            
            log.info("ProductRankScore 조회 완료: totalCount={}", scores.size());
        }

        if (scoreIterator.hasNext()) {
            return scoreIterator.next();
        }

        return null; // 더 이상 읽을 데이터가 없음
    }
}

