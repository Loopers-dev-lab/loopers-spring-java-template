package com.loopers.domain.product;

import com.loopers.domain.Money;
import com.loopers.domain.brand.Brand;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@RequiredArgsConstructor
@Component
public class ProductService {

    private final ProductRepository productRepository;

    public Product registerProduct(String productCode, String productName, BigDecimal price, int stock, Brand brand) {
        if (productCode == null || productCode.trim().isEmpty()) {
            throw new CoreException(ErrorType.BAD_REQUEST, "상품 코드는 필수값입니다");
        }
        validateProductCodeNotDuplicated(productCode);

        Product product = Product.createProduct(productCode, productName, Money.of(price), stock, brand);
        return productRepository.registerProduct(product);
    }

    public List<Product> getProducts(ProductSortType sortType) {
        ProductSortType appliedSortType = (sortType != null) ? sortType : ProductSortType.LATEST;
        return productRepository.findAllBySortType(appliedSortType);
    }

    public Product getProductDetail(Long productId) {
        return productRepository.findByIdWithBrand(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다"));
    }

    private void validateProductCodeNotDuplicated(String productCode) {
        if (productRepository.existsProductCode(productCode)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "중복된 상품 코드 오류");
        }
    }
}
