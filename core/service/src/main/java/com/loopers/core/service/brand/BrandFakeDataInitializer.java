package com.loopers.core.service.brand;

import com.loopers.core.domain.brand.Brand;
import com.loopers.core.domain.brand.repository.BrandRepository;
import com.loopers.core.domain.brand.vo.BrandDescription;
import com.loopers.core.domain.brand.vo.BrandName;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Random;

@Component
@Profile("local")
public class BrandFakeDataInitializer {

    private static final int TOTAL_BRAND_COUNT = 10;

    // 브랜드명 목록
    private static final String[] BRAND_NAMES = {
            "TechCore", "EliteGear", "ProMax", "Nexus", "Quantum",
            "Zenith", "Infinity", "Velocity", "Horizon", "Eclipse"
    };

    // 브랜드 설명 목록
    private static final String[] BRAND_DESCRIPTIONS = {
            "최신 기술로 무장한 프리미엄 브랜드",
            "전문가를 위한 고급 제품 라인",
            "성능과 품질의 최고봉",
            "혁신적인 디자인의 선구자",
            "신뢰할 수 있는 글로벌 브랜드",
            "차별화된 기술력으로 업계 선도",
            "고객 중심의 서비스 철학",
            "지속 가능한 미래를 추구",
            "뛰어난 가성비의 대표 주자",
            "고급스럽고 세련된 이미지"
    };

    private final BrandRepository brandRepository;
    private final Random random = new Random();

    public BrandFakeDataInitializer(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    @PostConstruct
    public void initializeBrandData() {
        long startTime = System.currentTimeMillis();
        System.out.println("🚀 브랜드 데이터 초기화 시작 (" + TOTAL_BRAND_COUNT + "건)...");

        for (int i = 0; i < TOTAL_BRAND_COUNT; i++) {
            Brand brand = Brand.create(
                    new BrandName(BRAND_NAMES[i]),
                    new BrandDescription(BRAND_DESCRIPTIONS[i])
            );

            brandRepository.save(brand);
            System.out.println("✓ 브랜드 '" + BRAND_NAMES[i] + "' 저장 완료");
        }

        long endTime = System.currentTimeMillis();
        System.out.println("✅ 브랜드 데이터 초기화 완료! (" + (endTime - startTime) / 1000.0 + "초)");
    }
}
