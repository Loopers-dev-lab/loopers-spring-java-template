package com.loopers.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.money.Money;
import com.loopers.domain.order.orderitem.OrderItemCommand;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.ProductRepository;
import com.loopers.domain.quantity.Quantity;
import com.loopers.domain.stock.Stock;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import com.loopers.support.test.IntegrationTestSupport;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@DisplayName("OrderCreateService 통합 테스트")
class OrderCreateServiceTest extends IntegrationTestSupport {

  @Autowired
  private OrderCreateService orderCreateService;

  @Autowired
  private ProductRepository productRepository;

  @Autowired
  private BrandRepository brandRepository;

  private Brand brand;
  private Product product1;
  private Product product2;

  @BeforeEach
  void setUp() {
    brand = brandRepository.save(Brand.of("테스트브랜드"));
    product1 = productRepository.save(
        Product.of("상품1", Money.of(10000L), "설명1", Stock.of(100L), brand.getId())
    );
    product2 = productRepository.save(
        Product.of("상품2", Money.of(30000L), "설명2", Stock.of(50L), brand.getId())
    );
  }

  @Nested
  @DisplayName("주문 준비")
  class PrepareOrder {

    @Test
    @DisplayName("정상적으로 주문을 준비한다")
    void prepareOrder_success() {
      Map<Long, Product> productMap = Map.of(
          product1.getId(), product1,
          product2.getId(), product2
      );

      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(product1.getId(), Quantity.of(10L)),
          OrderItemCommand.of(product2.getId(), Quantity.of(5L))
      );

      OrderPreparation result = orderCreateService.prepareOrder(commands, productMap);

      assertThat(result.orderItems()).isNotNull();
      assertThat(result.totalAmount()).isEqualTo(250000L);
      assertThat(product1.getStockValue()).isEqualTo(100L);
      assertThat(product2.getStockValue()).isEqualTo(50L);
    }

    @Test
    @DisplayName("상품을 찾을 수 없으면 예외 발생")
    void prepareOrder_productNotFound() {
      Map<Long, Product> productMap = Map.of();

      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(999L, Quantity.of(10L))
      );

      assertThatThrownBy(() -> orderCreateService.prepareOrder(commands, productMap))
          .isInstanceOf(CoreException.class)
          .hasMessage("상품을 찾을 수 없습니다.")
          .extracting(ex -> ((CoreException) ex).getErrorType())
          .isEqualTo(ErrorType.NOT_FOUND);
    }

    @Test
    @DisplayName("재고가 부족하면 예외 발생")
    void prepareOrder_insufficientStock() {
      Product lowStockProduct = productRepository.save(
          Product.of("재고부족상품", Money.of(10000L), "설명", Stock.of(5L), brand.getId())
      );

      Map<Long, Product> productMap = Map.of(lowStockProduct.getId(), lowStockProduct);

      List<OrderItemCommand> commands = List.of(
          OrderItemCommand.of(lowStockProduct.getId(), Quantity.of(10L))
      );

      assertThatThrownBy(() -> orderCreateService.prepareOrder(commands, productMap))
          .isInstanceOf(CoreException.class)
          .extracting(ex -> ((CoreException) ex).getErrorType())
          .isEqualTo(ErrorType.INSUFFICIENT_STOCK);
    }
  }
}
