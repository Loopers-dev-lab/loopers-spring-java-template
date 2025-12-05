package com.loopers.performance.data;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class BulkDataGenerator {
    private final ProductDataGenerator productGenerator;

    @Autowired
    public BulkDataGenerator(ProductDataGenerator productGenerator) {
        this.productGenerator = productGenerator;
    }

    @Transactional
    public void generateRound5Data(int productCount, int brandCount) {
        System.out.println("=== Round 5 대용량 데이터 생성 시작 ===");
        System.out.println("상품 수: " + productCount + ", 브랜드 수: " + brandCount);
        long totalStartTime = System.currentTimeMillis();

        List<Long> productIds = productGenerator.generateProducts(
                productCount,
                brandCount,
                1000  // 배치 크기
        );

        long totalElapsed = System.currentTimeMillis() - totalStartTime;
        System.out.println("=== Round 5 데이터 생성 완료 ===");
        System.out.println("총 소요 시간: " + totalElapsed + "ms (" + (totalElapsed / 1000.0) + "초)");
        System.out.println("생성된 상품 수: " + productIds.size());
    }
}

