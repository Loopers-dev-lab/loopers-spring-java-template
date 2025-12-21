package com.loopers.domain.product;

import com.loopers.domain.common.event.DomainEventPublisher;
import com.loopers.domain.order.orderitem.OrderItem;
import com.loopers.domain.product.event.ProductOutOfStockEvent;
import com.loopers.domain.product.event.ProductSoldEvent;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class ProductService {

  private final ProductRepository productRepository;
  private final DomainEventPublisher eventPublisher;
  private final Clock clock;

  public Optional<Product> getById(Long productId) {
    return productRepository.findById(productId);
  }

  @Transactional
  public List<Product> findByIdsWithLock(List<Long> productIds) {
    Objects.requireNonNull(productIds, "상품 ID 목록은 null일 수 없습니다");
    List<Long> distinctIds = productIds.stream().distinct().toList();
    return productRepository.findAllByIdWithLock(distinctIds);
  }

  public Page<Product> findProducts(Long brandId, Pageable pageable) {
    return brandId != null
        ? productRepository.findByBrandId(brandId, pageable)
        : productRepository.findAll(pageable);
  }

  public Product create(Product product) {
    return productRepository.saveAndFlush(product);
  }

  @Transactional
  public void increaseLikeCount(Long productId) {
    productRepository.incrementLikeCount(productId);
  }

  @Transactional
  public void decreaseLikeCount(Long productId) {
    productRepository.decrementLikeCount(productId);
  }

  @Transactional
  public StockDecreaseResult tryDecreaseStocks(List<OrderItem> orderItems, Long orderId) {
    Map<Long, Long> quantityByProductId = sumQuantityByProductId(orderItems);

    List<Product> products = productRepository.findAllByIdWithLock(
        new ArrayList<>(quantityByProductId.keySet()));

    if (hasMissingProducts(products, quantityByProductId)) {
      List<Long> missingIds = findMissingProductIds(products, quantityByProductId);
      return StockDecreaseResult.failure(missingIds);
    }

    List<Long> insufficientProductIds = findInsufficientProducts(products, quantityByProductId);
    if (!insufficientProductIds.isEmpty()) {
      return StockDecreaseResult.failure(insufficientProductIds);
    }

    decreaseAndPublishEvents(products, quantityByProductId, orderId);

    return StockDecreaseResult.success();
  }

  private boolean hasMissingProducts(List<Product> products, Map<Long, Long> quantityByProductId) {
    return products.size() != quantityByProductId.size();
  }

  private List<Long> findMissingProductIds(
      List<Product> products, Map<Long, Long> quantityByProductId) {
    var foundIds = products.stream().map(Product::getId).collect(Collectors.toSet());
    return quantityByProductId.keySet().stream()
        .filter(id -> !foundIds.contains(id))
        .toList();
  }

  private Map<Long, Long> sumQuantityByProductId(List<OrderItem> orderItems) {
    return orderItems.stream()
        .collect(Collectors.groupingBy(
            OrderItem::getProductId,
            Collectors.summingLong(OrderItem::getQuantityValue)));
  }

  private List<Long> findInsufficientProducts(
      List<Product> products, Map<Long, Long> quantityByProductId) {
    return products.stream()
        .filter(p -> !p.hasEnoughStock(quantityByProductId.get(p.getId())))
        .map(Product::getId)
        .toList();
  }

  private void decreaseAndPublishEvents(
      List<Product> products, Map<Long, Long> quantityByProductId, Long orderId) {
    LocalDateTime now = LocalDateTime.now(clock);

    for (Product product : products) {
      Long quantity = quantityByProductId.get(product.getId());
      boolean wasAvailable = product.isAvailable();

      product.decreaseStock(quantity);

      // 판매 이벤트 발행
      eventPublisher.publish(
          ProductSoldEvent.of(product.getId(), orderId, Math.toIntExact(quantity), now));

      // 재고 소진 이벤트 발행
      boolean becameSoldOut = wasAvailable && product.isNotAvailable();
      if (becameSoldOut) {
        eventPublisher.publish(ProductOutOfStockEvent.of(product.getId(), now));
      }
    }
  }
}
