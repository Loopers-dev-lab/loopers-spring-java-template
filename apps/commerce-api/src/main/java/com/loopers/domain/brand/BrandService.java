package com.loopers.domain.brand;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.loopers.domain.brand.dto.BrandSearchFilter;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;

import lombok.RequiredArgsConstructor;

import jakarta.validation.Valid;

/**
 * 브랜드 도메인 서비스
 * <p>
 * 브랜드 도메인의 비즈니스 로직을 처리합니다.
 * 단일 책임 원칙에 따라 브랜드 Repository에만 의존합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 9.
 */
@RequiredArgsConstructor
@Component
public class BrandService {
    private final BrandRepository brandRepository;

    /**
     * 브랜드 목록을 조회합니다.
     *
     * @param pageable 페이징 정보
     * @return 브랜드 목록 페이지
     */
    @Transactional(readOnly = true)
    public Page<BrandEntity> listBrands(Pageable pageable) {
        return brandRepository.listBrands(pageable);
    }

    /**
     * 브랜드 ID로 브랜드를 조회합니다.
     *
     * @param id 브랜드 ID
     * @return 조회된 브랜드 엔티티
     * @throws CoreException 브랜드를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public BrandEntity getBrandById(long id) {
        return brandRepository.getBrandById(id)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND_BRAND,
                        String.format("브랜드를 찾을 수 없습니다. (ID: %d)", id)
                ));
    }

    /**
     * 브랜드 이름으로 브랜드를 조회합니다.
     *
     * @param name 브랜드 이름
     * @return 조회된 브랜드 엔티티
     * @throws CoreException 브랜드를 찾을 수 없는 경우
     */
    @Transactional(readOnly = true)
    public BrandEntity getBrandByName(String name) {
        return brandRepository.findByName(name)
                .orElseThrow(() -> new CoreException(
                        ErrorType.NOT_FOUND_BRAND,
                        String.format("브랜드를 찾을 수 없습니다. (이름: %s)", name)
                ));
    }

    /**
     * 검색 필터 조건으로 브랜드를 조회합니다.
     *
     * @param filter   검색 필터
     * @param pageable 페이징 정보
     * @return 검색된 브랜드 목록 페이지
     */
    @Transactional(readOnly = true)
    public Page<BrandEntity> searchBrands(BrandSearchFilter filter, Pageable pageable) {
        return brandRepository.searchBrands(filter, pageable);
    }

    /**
     * 브랜드를 등록합니다.
     * <p>
     * 브랜드 등록은 단일 도메인 작업이므로 도메인 서비스에서 트랜잭션 처리합니다.
     *
     * @param request 브랜드 생성 요청 정보
     * @return 등록된 브랜드 엔티티
     * @throws CoreException 중복된 브랜드 이름이 존재하는 경우
     */
    @Transactional
    public BrandEntity registerBrand(@Valid BrandDomainCreateRequest request) {
        // 중복 브랜드명 검증
        brandRepository.findByName(request.name())
                .ifPresent(existingBrand -> {
                    throw new CoreException(
                            ErrorType.DUPLICATE_BRAND,
                            String.format("이미 존재하는 브랜드 이름입니다. (이름: %s)", request.name())
                    );
                });

        // 브랜드 엔티티 생성
        BrandEntity brandEntity = BrandEntity.createBrandEntity(request);

        return brandRepository.save(brandEntity);
    }
}
