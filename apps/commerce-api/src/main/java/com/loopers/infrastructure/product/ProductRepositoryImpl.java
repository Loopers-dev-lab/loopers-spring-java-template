package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductSortType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Component
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository productJpaRepository;

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findByIdAndNotDeleted(id);
    }

    @Override
    public Page<Product> findAll(ProductSortType sortType, int page, int size) {

        Sort sort = createSort(sortType);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        return productJpaRepository.findAll(pageRequest);
    }

    @Override
    public List<Product> findByBrandId(Long brandId) {
        return productJpaRepository.findByBrandId(brandId);
    }

    @Override
    public Page<Product> findByBrandId(Long brandId, ProductSortType sortType, int page, int size) {

        Sort sort = createSort(sortType);
        PageRequest pageRequest = PageRequest.of(page, size, sort);

        return productJpaRepository.findByBrandId(brandId, pageRequest);
    }

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public List<Product> findAllByIdIn(List<Long> ids) {
        return productJpaRepository.findAllById(ids);
    }

    @Override
    public Optional<Product> findByIdWithLock(Long id) {
        return productJpaRepository.findByIdWithLock(id);
    }

    @Override
    public void flush() {
        productJpaRepository.flush();
    }

    private Sort createSort(ProductSortType sortType) {
        return switch (sortType) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price");
            case LIKES_DESC -> Sort.by(Sort.Direction.DESC, "totalLikes");
        };
    }
}
