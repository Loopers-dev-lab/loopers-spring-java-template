package com.loopers.infrastructure.brand;

import com.loopers.domain.brand.BrandModel;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.brand.QBrandModel;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class BrandRepositortyImpl implements BrandRepository {
    private final BrandJpaRepository brandJpaRepository;
    public final JPAQueryFactory queryFactory;

    public BrandRepositortyImpl(BrandJpaRepository brandJpaRepository, JPAQueryFactory queryFactory) {
        this.brandJpaRepository = brandJpaRepository;
        this.queryFactory = queryFactory;
    }

    @Override
    public BrandModel save(BrandModel brandModel) {
        return brandJpaRepository.save(brandModel);
    }

    @Override
    public Optional<BrandModel> findById(Long id) {
        if (id == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "브랜드 ID는 null일 수 없습니다.");
        }
        return brandJpaRepository.findById(id);
    }

    @Override
    public List<BrandModel> findByBrandIds(List<Long> brandIds) {
        if(brandIds.isEmpty() || brandIds == null) {
            throw new CoreException(ErrorType.BAD_REQUEST, "조회할 수 있는 brandId가 없습니다.");
        }
        QBrandModel brand = QBrandModel.brandModel;
        return queryFactory.select(brand)
                .from(brand)
                .where(brand.id.in(brandIds))
                .fetch();
    }

    @Override
    public List<BrandModel> findByIdIn(List<Long> brandIds) {
        if (brandIds == null || brandIds.isEmpty()) {
            return List.of();
        }
        return brandJpaRepository.findByIdIn(brandIds);
    }

    @Override
    public void deleteAll() {
        brandJpaRepository.deleteAll();
    }

}
