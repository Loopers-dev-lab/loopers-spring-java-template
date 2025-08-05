package com.loopers.domain.order;

import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class OrderItemModelTest {

    @Nested
    @DisplayName("주문 아이템 생성 관련 테스트")
    class CreateTest {

        @DisplayName("정상적인 값으로 주문 아이템을 생성할 수 있다")
        @Test
        void create_withValidValues() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();

            // act
            OrderItemModel orderItem = OrderItemFixture.createOrderItem(orderModel);

            // assert
            assertAll(
                    () -> assertThat(orderItem).isNotNull(),
                    () -> assertThat(orderItem.getOrderModel()).isEqualTo(orderModel),
                    () -> assertThat(orderItem.getProductId().getValue()).isEqualTo(OrderItemFixture.ORDER_ITEM_PRODUCT_ID),
                    () -> assertThat(orderItem.getOptionId().getValue()).isEqualTo(OrderItemFixture.ORDER_ITEM_OPTION_ID),
                    () -> assertThat(orderItem.getQuantity().getValue()).isEqualTo(OrderItemFixture.ORDER_ITEM_QUANTITY),
                    () -> assertThat(orderItem.getOrderItemPrice().getValue()).isEqualTo(OrderItemFixture.ORDER_ITEM_PRICE_PER_UNIT),
                    () -> assertThat(orderItem.getProductSnapshot().getProductName()).isEqualTo(OrderItemFixture.ORDER_ITEM_PRODUCT_NAME),
                    () -> assertThat(orderItem.getProductSnapshot().getOptionName()).isEqualTo(OrderItemFixture.ORDER_ITEM_OPTION_NAME),
                    () -> assertThat(orderItem.getProductSnapshot().getImageUrl()).isEqualTo(OrderItemFixture.ORDER_ITEM_IMAGE_URL)
            );
        }

        @DisplayName("상품 ID가 null이면 생성에 실패한다")
        @Test
        void create_whenProductIdNull() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            Long productId = null;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItemFixture.createWithProductId(orderModel, productId);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("id cannot be null");
        }

        @DisplayName("옵션 ID가 null이면 생성에 실패한다")
        @Test
        void create_whenOptionIdNull() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            Long optionId = null;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItemFixture.createWithOptionId(orderModel, optionId);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("orderItemOptionId cannot be null");
        }

        @DisplayName("수량이 null이면 생성에 실패한다")
        @Test
        void create_whenQuantityNull() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            BigDecimal quantity = null;

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItemFixture.createWithQuantity(orderModel, quantity);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("수량은 필수입니다");
        }

        @DisplayName("음수 수량으로 생성에 실패한다")
        @Test
        void create_withNegativeQuantity() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            BigDecimal quantity = new BigDecimal("-1");

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItemFixture.createWithQuantity(orderModel, quantity);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("수량은 1 이상이어야 합니다");
        }

        @DisplayName("수량이 999를 초과하면 생성에 실패한다")
        @Test
        void create_withQuantityOverLimit() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            BigDecimal quantity = new BigDecimal("1000");

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItemFixture.createWithQuantity(orderModel, quantity);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("수량은 999개를 초과할 수 없습니다");
        }

        @DisplayName("음수 단가로 생성에 실패한다")
        @Test
        void create_withNegativePrice() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            BigDecimal pricePerUnit = new BigDecimal("-1000");

            // act & assert
            CoreException exception = assertThrows(CoreException.class, () -> {
                OrderItemFixture.createWithPrice(orderModel, pricePerUnit);
            });

            assertThat(exception.getErrorType()).isEqualTo(ErrorType.BAD_REQUEST);
            assertThat(exception.getMessage()).contains("단가는 0 이상이어야 합니다");
        }

        @DisplayName("상품명이 null이면 생성에 실패한다")
        @Test
        void create_whenProductNameNull() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            String productName = null;

            // act & assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                OrderItemFixture.createWithProductName(orderModel, productName);
            });

            assertThat(exception.getMessage()).contains("상품명은 필수입니다");
        }

        @DisplayName("빈 상품명으로 생성에 실패한다")
        @Test
        void create_whenProductNameEmpty() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            String productName = "   ";

            // act & assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                OrderItemFixture.createWithProductName(orderModel, productName);
            });

            assertThat(exception.getMessage()).contains("상품명은 필수입니다");
        }
    }

    @Nested
    @DisplayName("소계 계산 관련 테스트")
    class SubtotalTest {

        @DisplayName("소계를 올바르게 계산할 수 있다")
        @Test
        void subtotal_calculate() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            BigDecimal quantity = new BigDecimal("3");
            BigDecimal pricePerUnit = new BigDecimal("15000");
            OrderItemModel orderItem = OrderItemFixture.createOrderItem(
                    orderModel, 1L, 1L, quantity, pricePerUnit,
                    "Test Product", "Test Option", "http://example.com/image.jpg"
            );
            BigDecimal expectedSubtotal = quantity.multiply(pricePerUnit);

            // act
            BigDecimal subtotal = orderItem.subtotal();

            // assert
            assertThat(subtotal).isEqualByComparingTo(expectedSubtotal);
        }

        @DisplayName("수량이 0일 때 소계는 0이다")
        @Test
        void subtotal_withZeroQuantity() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            BigDecimal quantity = BigDecimal.ZERO;
            BigDecimal pricePerUnit = new BigDecimal("15000");
            OrderItemModel orderItem = OrderItemFixture.createOrderItem(
                    orderModel, 1L, 1L, quantity, pricePerUnit,
                    "Test Product", "Test Option", "http://example.com/image.jpg"
            );

            // act
            BigDecimal subtotal = orderItem.subtotal();

            // assert
            assertThat(subtotal).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @DisplayName("단가가 0일 때 소계는 0이다")
        @Test
        void subtotal_withZeroPrice() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            BigDecimal quantity = new BigDecimal("5");
            BigDecimal pricePerUnit = BigDecimal.ZERO;
            OrderItemModel orderItem = OrderItemFixture.createOrderItem(
                    orderModel, 1L, 1L, quantity, pricePerUnit,
                    "Test Product", "Test Option", "http://example.com/image.jpg"
            );

            // act
            BigDecimal subtotal = orderItem.subtotal();

            // assert
            assertThat(subtotal).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @DisplayName("기본 Fixture로 생성된 아이템의 소계를 계산할 수 있다")
        @Test
        void subtotal_withFixture() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            OrderItemModel orderItem = OrderItemFixture.createOrderItem(orderModel);
            BigDecimal expectedSubtotal = OrderItemFixture.ORDER_ITEM_QUANTITY
                    .multiply(OrderItemFixture.ORDER_ITEM_PRICE_PER_UNIT);

            // act
            BigDecimal subtotal = orderItem.subtotal();

            // assert
            assertThat(subtotal).isEqualByComparingTo(expectedSubtotal);
        }
    }

    @Nested
    @DisplayName("상품 스냅샷 설정 관련 테스트")
    class ProductSnapshotTest {

        @DisplayName("상품 스냅샷을 설정할 수 있다")
        @Test
        void setProductSnapshot_success() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            OrderItemModel orderItem = OrderItemFixture.createOrderItem(orderModel);
            String newProductName = "Updated Product";
            String newOptionName = "Updated Option";
            String newImageUrl = "http://example.com/updated-image.jpg";

            // act
            orderItem.setProductSnapshot(newProductName, newOptionName, newImageUrl);

            // assert
            assertAll(
                    () -> assertThat(orderItem.getProductSnapshot().getProductName()).isEqualTo(newProductName),
                    () -> assertThat(orderItem.getProductSnapshot().getOptionName()).isEqualTo(newOptionName),
                    () -> assertThat(orderItem.getProductSnapshot().getImageUrl()).isEqualTo(newImageUrl),
                    () -> assertThat(orderItem.getProductSnapshot().getPriceAtOrder()).isEqualTo(orderItem.getOrderItemPrice().getValue())
            );
        }

        @DisplayName("null 상품명으로 스냅샷 설정 시 실패한다")
        @Test
        void setProductSnapshot_withNullProductName() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            OrderItemModel orderItem = OrderItemFixture.createOrderItem(orderModel);
            String productName = null;
            String optionName = "Updated Option";
            String imageUrl = "http://example.com/updated-image.jpg";

            // act & assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                orderItem.setProductSnapshot(productName, optionName, imageUrl);
            });

            assertThat(exception.getMessage()).contains("상품명은 필수입니다");
        }

        @DisplayName("빈 상품명으로 스냅샷 설정 시 실패한다")
        @Test
        void setProductSnapshot_withEmptyProductName() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            OrderItemModel orderItem = OrderItemFixture.createOrderItem(orderModel);
            String productName = "   ";
            String optionName = "Updated Option";
            String imageUrl = "http://example.com/updated-image.jpg";

            // act & assert
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                orderItem.setProductSnapshot(productName, optionName, imageUrl);
            });

            assertThat(exception.getMessage()).contains("상품명은 필수입니다");
        }
    }

    @Nested
    @DisplayName("주문 소속 확인 관련 테스트")
    class BelongsToOrderTest {

        @DisplayName("주문 아이템이 특정 주문에 속하는지 확인할 수 있다")
        @Test
        void belongsToOrder_check() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            OrderItemModel orderItem = OrderItemFixture.createOrderItem(orderModel);

            // act & assert
            assertAll(
                    () -> assertThat(orderItem.belongsToOrder(orderModel.getId())).isTrue(),
                    () -> assertThat(orderItem.belongsToOrder(999L)).isFalse()
            );
        }
    }

    @Nested
    @DisplayName("Fixture를 사용한 테스트")
    class FixtureTest {

        @DisplayName("기본 Fixture로 주문 아이템을 생성할 수 있다")
        @Test
        void createWithDefaultFixture() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();

            // act
            OrderItemModel orderItem = OrderItemFixture.createOrderItem(orderModel);

            // assert
            assertAll(
                    () -> assertThat(orderItem).isNotNull(),
                    () -> assertThat(orderItem.getOrderModel()).isEqualTo(orderModel),
                    () -> assertThat(orderItem.getProductId().getValue()).isEqualTo(OrderItemFixture.ORDER_ITEM_PRODUCT_ID),
                    () -> assertThat(orderItem.getOptionId().getValue()).isEqualTo(OrderItemFixture.ORDER_ITEM_OPTION_ID),
                    () -> assertThat(orderItem.getQuantity().getValue()).isEqualTo(OrderItemFixture.ORDER_ITEM_QUANTITY),
                    () -> assertThat(orderItem.getOrderItemPrice().getValue()).isEqualTo(OrderItemFixture.ORDER_ITEM_PRICE_PER_UNIT),
                    () -> assertThat(orderItem.getProductSnapshot().getProductName()).isEqualTo(OrderItemFixture.ORDER_ITEM_PRODUCT_NAME),
                    () -> assertThat(orderItem.getProductSnapshot().getOptionName()).isEqualTo(OrderItemFixture.ORDER_ITEM_OPTION_NAME),
                    () -> assertThat(orderItem.getProductSnapshot().getImageUrl()).isEqualTo(OrderItemFixture.ORDER_ITEM_IMAGE_URL)
            );
        }

        @DisplayName("특정 상품명으로 아이템 Fixture를 생성할 수 있다")
        @Test
        void createWithSpecificProductName() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            String productName = "Custom Product Name";

            // act
            OrderItemModel orderItem = OrderItemFixture.createWithProductName(orderModel, productName);

            // assert
            assertThat(orderItem.getProductSnapshot().getProductName()).isEqualTo(productName);
        }

        @DisplayName("특정 옵션명으로 아이템 Fixture를 생성할 수 있다")
        @Test
        void createWithSpecificOptionName() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            String optionName = "Custom Option Name";

            // act
            OrderItemModel orderItem = OrderItemFixture.createWithOptionName(orderModel, optionName);

            // assert
            assertThat(orderItem.getProductSnapshot().getOptionName()).isEqualTo(optionName);
        }

        @DisplayName("특정 이미지 URL로 아이템 Fixture를 생성할 수 있다")
        @Test
        void createWithSpecificImageUrl() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            String imageUrl = "http://example.com/custom-image.jpg";

            // act
            OrderItemModel orderItem = OrderItemFixture.createWithImageUrl(orderModel, imageUrl);

            // assert
            assertThat(orderItem.getProductSnapshot().getImageUrl()).isEqualTo(imageUrl);
        }

        @DisplayName("특정 가격으로 아이템 Fixture를 생성할 수 있다")
        @Test
        void createWithSpecificPrice() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            BigDecimal price = new BigDecimal("25000");

            // act
            OrderItemModel orderItem = OrderItemFixture.createWithPrice(orderModel, price);

            // assert
            assertThat(orderItem.getOrderItemPrice().getValue()).isEqualByComparingTo(price);
        }

        @DisplayName("특정 수량으로 아이템 Fixture를 생성할 수 있다")
        @Test
        void createWithSpecificQuantity() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            BigDecimal quantity = new BigDecimal("5");

            // act
            OrderItemModel orderItem = OrderItemFixture.createWithQuantity(orderModel, quantity);

            // assert
            assertThat(orderItem.getQuantity().getValue()).isEqualByComparingTo(quantity);
        }

        @DisplayName("특정 상품 ID로 아이템 Fixture를 생성할 수 있다")
        @Test
        void createWithSpecificProductId() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            Long productId = 999L;

            // act
            OrderItemModel orderItem = OrderItemFixture.createWithProductId(orderModel, productId);

            // assert
            assertThat(orderItem.getProductId().getValue()).isEqualTo(productId);
        }

        @DisplayName("특정 옵션 ID로 아이템 Fixture를 생성할 수 있다")
        @Test
        void createWithSpecificOptionId() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            Long optionId = 888L;

            // act
            OrderItemModel orderItem = OrderItemFixture.createWithOptionId(orderModel, optionId);

            // assert
            assertThat(orderItem.getOptionId().getValue()).isEqualTo(optionId);
        }

        @DisplayName("커스텀 값들로 아이템 Fixture를 생성할 수 있다")
        @Test
        void createWithCustomValues() {
            // arrange
            OrderModel orderModel = OrderFixture.createOrderModel();
            Long productId = 123L;
            Long optionId = 456L;
            BigDecimal quantity = new BigDecimal("7");
            BigDecimal pricePerUnit = new BigDecimal("35000");
            String productName = "Custom Product";
            String optionName = "Custom Option";
            String imageUrl = "http://example.com/custom.jpg";

            // act
            OrderItemModel orderItem = OrderItemFixture.createOrderItem(
                    orderModel, productId, optionId, quantity, pricePerUnit,
                    productName, optionName, imageUrl
            );

            // assert
            assertAll(
                    () -> assertThat(orderItem.getProductId().getValue()).isEqualTo(productId),
                    () -> assertThat(orderItem.getOptionId().getValue()).isEqualTo(optionId),
                    () -> assertThat(orderItem.getQuantity().getValue()).isEqualByComparingTo(quantity),
                    () -> assertThat(orderItem.getOrderItemPrice().getValue()).isEqualByComparingTo(pricePerUnit),
                    () -> assertThat(orderItem.getProductSnapshot().getProductName()).isEqualTo(productName),
                    () -> assertThat(orderItem.getProductSnapshot().getOptionName()).isEqualTo(optionName),
                    () -> assertThat(orderItem.getProductSnapshot().getImageUrl()).isEqualTo(imageUrl),
                    () -> assertThat(orderItem.subtotal()).isEqualByComparingTo(quantity.multiply(pricePerUnit))
            );
        }
    }
}
