package com.loopers.domain.product;

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

    public Product registerProduct(String productCode, String productName, BigDecimal price, int stock) {
        validateProductCodeNotDuplicated(productCode);

        Product product = Product.createProduct(productCode, productName, price, stock);

        return productRepository.registerProduct(product);
    }

    public List<Product> getProducts(ProductSortType sortType) {
        ProductSortType appliedSortType = (sortType != null) ? sortType : ProductSortType.LATEST;
        return productRepository.findAllBySortType(appliedSortType);
    }

    private void validateProductCodeNotDuplicated(String productCode) {
        if (productRepository.existsProductCode(productCode)) {
            throw new CoreException(ErrorType.BAD_REQUEST, "중복된 상품 코드 오류");
        }
    }
}
