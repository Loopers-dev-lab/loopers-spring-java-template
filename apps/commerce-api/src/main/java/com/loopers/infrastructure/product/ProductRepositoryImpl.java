package com.loopers.infrastructure.product;

import com.loopers.domain.brand.Brand;
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
    public List<Product> findAll(ProductSortType sortType, int page, int size) {

        Sort sort = createSort(sortType);
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<Product> productPage = productJpaRepository.findAll(pageRequest);

        return productPage.getContent();
    }

    @Override
    public List<Product> findByBrandId(Long brandId) {
        return productJpaRepository.findByBrandId(brandId);
    }

    @Override
    public List<Product> findByBrandId(Long brandId, ProductSortType sortType, int page, int size) {

        Sort sort = createSort(sortType);
        PageRequest pageRequest = PageRequest.of(page, size, sort);
        Page<Product> productPage = productJpaRepository.findByBrandId(brandId, pageRequest);

        return productPage.getContent();
    }

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    private Sort createSort(ProductSortType sortType) {
        return switch (sortType) {
            case LATEST -> Sort.by(Sort.Direction.DESC, "createdAt");
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price");
            case LIKES_DESC -> Sort.by(Sort.Direction.DESC, "likeCount");
        };
    }
}
