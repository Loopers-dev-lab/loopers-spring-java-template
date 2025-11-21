package com.loopers.fixtures;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.loopers.domain.brand.BrandDomainCreateRequest;
import com.loopers.domain.brand.BrandEntity;
import com.loopers.domain.brand.BrandRepository;

/**
 * 브랜드 테스트 픽스처
 *
 * 테스트에서 사용할 브랜드 엔티티 및 요청 객체를 생성합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 12.
 */
public class BrandTestFixture {

    /**
     * BrandDomainCreateRequest를 생성합니다.
     *
     * @param name 브랜드 이름
     * @param description 브랜드 설명
     * @return BrandDomainCreateRequest
     */
    public static BrandDomainCreateRequest createRequest(String name, String description) {
        return BrandDomainCreateRequest.of(name, description);
    }

    /**
     * BrandEntity를 생성합니다.
     *
     * @param name 브랜드 이름
     * @param description 브랜드 설명
     * @return BrandEntity
     */
    public static BrandEntity createEntity(String name, String description) {
        BrandDomainCreateRequest request = createRequest(name, description);
        return BrandEntity.createBrandEntity(request);
    }

    /**
     * 여러 개의 BrandEntity를 생성합니다.
     *
     * @param count 생성할 개수
     * @return BrandEntity 리스트
     */
    public static List<BrandEntity> createEntities(int count) {
        return IntStream.range(0, count)
                .mapToObj(i -> createEntity("브랜드" + i, "설명" + i))
                .collect(Collectors.toList());
    }

    /**
     * 여러 개의 브랜드를 생성하고 저장합니다.
     *
     * @param repository 브랜드 레포지토리
     * @param count 생성할 개수
     * @return 저장된 BrandEntity 리스트
     */
    public static List<BrandEntity> saveBrands(BrandRepository repository, int count) {
        return createEntities(count).stream()
                .map(repository::save)
                .collect(Collectors.toList());
    }

    /**
     * 브랜드를 생성하고 저장합니다.
     *
     * @param repository 브랜드 레포지토리
     * @param name 브랜드 이름
     * @param description 브랜드 설명
     * @return 저장된 BrandEntity
     */
    public static BrandEntity createAndSave(BrandRepository repository, String name, String description) {
        BrandEntity brand = createEntity(name, description);
        return repository.save(brand);
    }
}
