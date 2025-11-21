package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Page<Product> getProducts(Long brandId, Pageable pageable, String sort) {
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ProductSortType.toSort(sort)
        );
        return productRepository.findProducts(brandId, pageRequest);
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow(
                () -> new IllegalArgumentException("존재하지 않는 상품입니다.")
        );
    }

    public List<Product> findAll(List<Long> productIds) {
        return productRepository.findAllById(productIds);
    }

    public Map<Long, Product> getProductsMapByIds(List<Long> productIds) {
        List<Product> products = productRepository.findAllById(productIds);

        if(products.isEmpty()) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.");
        }
        return products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }
}
