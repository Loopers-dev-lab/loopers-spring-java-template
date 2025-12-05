package com.loopers.performance.fixture;

import com.loopers.performance.data.BulkDataGenerator;
import com.loopers.testcontainers.MySqlTestContainersConfig;
import com.loopers.utils.DatabaseCleanUp;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import({MySqlTestContainersConfig.class})
public abstract class PerformanceTestFixture {
    protected final BulkDataGenerator bulkDataGenerator;
    protected final DatabaseCleanUp databaseCleanUp;

    @Autowired
    public PerformanceTestFixture(
            BulkDataGenerator bulkDataGenerator,
            DatabaseCleanUp databaseCleanUp
    ) {
        this.bulkDataGenerator = bulkDataGenerator;
        this.databaseCleanUp = databaseCleanUp;
    }

    protected void prepareTestData(int productCount, int brandCount) {
        System.out.println("성능 테스트 데이터 준비 시작: 상품 " + productCount + "개, 브랜드 " + brandCount + "개");
        databaseCleanUp.truncateAllTables();
        bulkDataGenerator.generateRound5Data(productCount, brandCount);
        System.out.println("성능 테스트 데이터 준비 완료");
    }

    /**
     * 테스트 후 정리 (선택사항)
     * 필요시 하위 클래스에서 오버라이드
     */
    protected void cleanup() {
        // 기본 구현은 비어있음
        // 필요시 하위 클래스에서 구현
    }
}

