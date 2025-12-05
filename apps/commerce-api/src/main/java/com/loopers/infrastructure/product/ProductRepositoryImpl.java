package com.loopers.infrastructure.product;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    @Override
    public Product save(Product product) {
        return jpaRepository.save(product);
    }

    @Override
    public void saveAll(List<Product> products) {
        jpaRepository.saveAll(products);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return jpaRepository.findById(id);
    }

    @Override
    public Page<Product> findAll(Pageable pageable) {
        return jpaRepository.findAll(pageable);
    }

    @Override
    public List<Product> findAllByIds(Collection<Long> ids) {
        return jpaRepository.findAllByIds(ids);
    }

    @Override
    public Page<Product> findProducts(Pageable pageable, Long brandId) {
        return jpaRepository.findProducts(brandId, pageable);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public Optional<Product> findByIdWithPessimisticLock(Long id) {
        return jpaRepository.findByIdWithPessimisticLock(id);
    }

    @Override
    public List<Product> findAllByIdsWithPessimisticLock(Collection<Long> ids) {
        return jpaRepository.findAllByIdsWithPessimisticLock(ids);
    }
}
