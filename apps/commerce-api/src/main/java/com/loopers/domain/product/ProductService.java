package com.loopers.domain.product;

import com.loopers.application.product.ProductInfo;
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

    public Page<ProductInfo> getProducts(Long brandId, Pageable pageable, String sort) {
        Pageable pageRequest = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                ProductSortType.toSort(sort)
        );
        if (brandId != null) {
            return productRepository.findAllByBrandIdWithLikeCount(brandId, pageRequest).map(ProductInfo::from);
        } else {
            return productRepository.findAllWithLikeCount(pageRequest).map(ProductInfo::from);
        }
    }

    public Product getProduct(Long id) {
        return productRepository.findById(id).orElseThrow(
                () -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
        );
    }

    public Map<Long, Product> getProductsMapByIds(List<Long> productIds) {
        List<Product> products = productRepository.findAllById(productIds);

        if(products.isEmpty()) {
            throw new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.");
        }
        return products.stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    public ProductDetailProjection getProductDetail(Long productId) {

        return productRepository.findByIdWithBrandAndLikeCount(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다."));
    }
}
