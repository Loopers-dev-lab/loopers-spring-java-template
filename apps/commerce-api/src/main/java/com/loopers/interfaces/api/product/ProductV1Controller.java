package com.loopers.interfaces.api.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.product.ProductService;
import com.loopers.interfaces.api.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1/products")
public class ProductV1Controller {
    private final ProductService productService;
    private final ProductRepository productRepository;

    @GetMapping("")
    public ApiResponse<ProductV1Dto.ProductListResponse> getProducts() {
        List<Product> products = productRepository.findAll();
        List<ProductV1Dto.ProductListItem> items = products.stream()
            .map(p -> new ProductV1Dto.ProductListItem(
                p.getId(),
                p.getName(),
                p.getPrice().getAmount(),
                p.getStockQuantity(),
                p.getLikeCount()
            ))
            .collect(Collectors.toList());
        return ApiResponse.success(new ProductV1Dto.ProductListResponse(items));
    }

    @GetMapping("/{productId}")
    public ApiResponse<ProductV1Dto.ProductResponse> getProduct(
        @PathVariable("productId") Long productId
    ) {
        var info = productService.getProductDetail(productId);
        return ApiResponse.success(ProductV1Dto.ProductResponse.from(info));
    }
}
