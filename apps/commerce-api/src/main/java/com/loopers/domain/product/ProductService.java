package com.loopers.domain.product;

import com.loopers.domain.common.vo.Price;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
public class ProductService {
    private final ProductRepository productRepository;

    public Product getProductById(Long productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));
    }

    public List<Product> getProductsByIds(Collection<Long> productIds) {
        return productRepository.findAllByIdIn(productIds);
    }

    public Map<Long, Product> getProductMapByIds(Collection<Long> productIds) {
        return productRepository.findAllByIdIn(productIds)
                .stream()
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    public Page<Product> getProducts(Pageable pageable) {
        int page = pageable.getPageNumber();
        int size = pageable.getPageSize();
        String sortStr = pageable.getSort().toString().split(":")[0];
        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt");

        if (StringUtils.startsWith(sortStr, "price_asc")) {
            sort = Sort.by(Sort.Direction.ASC, "price");
        }
        return productRepository.findAll(PageRequest.of(page, size, sort));
    }

    public boolean existsById(Long productId) {
        return productRepository.existsById(productId);
    }
}
