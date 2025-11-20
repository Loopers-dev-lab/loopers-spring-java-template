package com.loopers.domain.product;

import com.loopers.application.order.OrderCommand;
import com.loopers.domain.product.exception.NotEnoughStockException;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@RequiredArgsConstructor
@Component
public class ProductService {
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public Page<ProductModel> getProductsNotStopSelling(ProductSortType sortType, Pageable pageable) {
        Sort sort = toSort(sortType);

        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        return productRepository.findByStatusNot(ProductStatus.STOP_SELLING, pageRequest);
    }

    @Transactional(readOnly = true)
    public Page<ProductModel> getBrandProducts(Long brandId, ProductSortType sortType, Pageable pageable) {
        Sort sort = toSort(sortType);
        Pageable pageRequest = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);

        return productRepository.findByBrandId(brandId, pageRequest);
    }

    @Transactional(readOnly = true)
    public ProductModel getProductDetail(Long productId) {
        ProductModel product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다. 다시 확인해주세요"));
        return product;
    }

    @Transactional
    public void increaseStock(Long productId, Integer quantity) {
        ProductModel product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다. 다시 확인해주세요"));
        product.increaseStock(quantity);
        productRepository.save(product);
    }

    /**
     * Use Pessimistic Lock
     * @param productId
     * @param quantity
     * @return
     */
    @Transactional
    public boolean decreaseStock(Long productId, Integer quantity) {
        ProductModel product = productRepository.findByIdForUpdate(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다. 다시 확인해주세요"));
        try {
            product.decreaseStock(quantity);
            return true;
        } catch (NotEnoughStockException e) {
            return false;
        }
    }

    public boolean hasStock(Long productId, Integer quantity) {
        ProductModel product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다. 다시 확인해주세요"));
        if(product.getStock() < quantity) {
            return false;
        }
        return true;
    }

    @Transactional(readOnly = true)
    public int getPrice(Long productId, Integer quantity) {
        ProductModel product = productRepository.findById(productId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다. 다시 확인해주세요"));
        return product.getPrice() * quantity;
    }


    public void validateProductDeleteOrStopSelling(ProductModel product) {
        if(product.isStatusOnDeletedOrStopSelling())
            throw new CoreException(ErrorType.BAD_REQUEST, "좋아요 할 수 없는 상품입니다");
    }


    private Sort toSort(ProductSortType sortType) {
        if(sortType == null || sortType == ProductSortType.DEFAULT || sortType == ProductSortType.LATEST) {
            return Sort.by(Sort.Direction.DESC, "createdAt", "id");
        }

        // 자바 14 문법
        return switch(sortType) {
            case PRICE_ASC -> Sort.by(Sort.Direction.ASC, "price", "id");
            case PRICE_DESC -> Sort.by(Sort.Direction.DESC, "price", "id");
            default -> Sort.by(Sort.Direction.DESC, "createdAt", "id");
        };
    }

    public void markCurrentStockStatus(List<OrderCommand.OrderLine> orderLines) {
        // TODO 클린아키텍처 점검
    }
}
