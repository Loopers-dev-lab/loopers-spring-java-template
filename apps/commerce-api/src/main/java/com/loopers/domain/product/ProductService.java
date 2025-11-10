package com.loopers.domain.product;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Component
public class ProductService {

    private final ProductRepository productRepository;

    public Product registerProduct(String productCode, String productName, BigDecimal price, int stock) {

        if( productRepository.existsProductCode(productCode) ) {
            throw new CoreException(ErrorType.BAD_REQUEST, "중복된 상품 코드 오류");
        }

        Product product = Product.builder()
                .productCode(productCode)
                .productName(productName)
                .price(price)
                .stock(stock)
                .build();

        return productRepository.registerProduct(product);
    }
}
