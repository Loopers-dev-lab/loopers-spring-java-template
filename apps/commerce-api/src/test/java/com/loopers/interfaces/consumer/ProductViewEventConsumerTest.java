package com.loopers.interfaces.consumer;

import com.loopers.domain.brand.Brand;
import com.loopers.domain.brand.BrandRepository;
import com.loopers.domain.product.ProductStatus;
import com.loopers.domain.product.event.ProductEvents;
import com.loopers.domain.product.view.ProductView;
import com.loopers.domain.product.view.ProductViewRepository;
import com.loopers.event.consumer.KafkaMessageProcessor;
import com.loopers.shared.event.DomainEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@DisplayName("ProductViewEventConsumer 단위 테스트")
@ExtendWith(MockitoExtension.class)
class ProductViewEventConsumerTest {

    @Mock
    private KafkaMessageProcessor messageProcessor;

    @Mock
    private ProductViewRepository productViewRepository;

    @Mock
    private BrandRepository brandRepository;

    @Mock
    private MeterRegistry meterRegistry;

    @Mock
    private Counter counter;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private KafkaProductViewEventConsumer consumer;

    @BeforeEach
    void setUp() {
        // KafkaMessageProcessor Mock 설정 - 비즈니스 로직 실행하도록
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            ConsumerRecord<String, DomainEvent> record = (ConsumerRecord<String, DomainEvent>) invocation.getArgument(0);
            @SuppressWarnings("unchecked")
            KafkaMessageProcessor.BusinessLogic<DomainEvent> businessLogic = (KafkaMessageProcessor.BusinessLogic<DomainEvent>) invocation.getArgument(3);
            businessLogic.execute(record.value());
            return null;
        }).when(messageProcessor).execute(any(), any(), anyString(), any());

        // MeterRegistry Mock 설정
        when(meterRegistry.counter(anyString(), any(String[].class))).thenReturn(counter);
    }

    private <T> ConsumerRecord<String, T> createConsumerRecord(String topic, T value) {
        return new ConsumerRecord<>(topic, 0, 0L, "key", value);
    }

    @DisplayName("handleCreated 테스트")
    @Nested
    class HandleCreatedTest {

        @DisplayName("성공 케이스: ProductView 생성")
        @Test
        void handleCreated_createsProductView() {
            // arrange
            Long productId = 1L;
            Long brandId = 10L;
            String brandName = "Test Brand";
            ProductEvents.Created event = new ProductEvents.Created(
                    productId,
                    brandId,
                    "Test Product",
                    BigDecimal.valueOf(10000),
                    ProductStatus.ON_SALE
            );

            Brand brand = Brand.builder()
                    .name(brandName)
                    .description("Test Description")
                    .status(com.loopers.domain.brand.BrandStatus.ON_SALE)
                    .isVisible(true)
                    .isSellable(true)
                    .build();

            when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
            when(productViewRepository.save(any(ProductView.class))).thenReturn(Optional.of(ProductView.builder().build()));

            ConsumerRecord<String, ProductEvents.Created> record = createConsumerRecord("product.created.v1", event);

            // act
            consumer.handleCreated(record, acknowledgment);

            // assert
            verify(brandRepository).findById(brandId);
            verify(productViewRepository).save(argThat(pv ->
                    pv.getId().equals(productId) &&
                    pv.getName().equals("Test Product") &&
                    pv.getPrice().compareTo(BigDecimal.valueOf(10000)) == 0 &&
                    pv.getBrandId().equals(brandId) &&
                    pv.getBrandName().equals(brandName) &&
                    pv.getStatus().equals(ProductStatus.ON_SALE) &&
                    pv.getLikeCount().equals(0L)
            ));
            verify(acknowledgment).acknowledge();
        }

        @DisplayName("성공 케이스: Brand가 없는 경우 brandName은 null")
        @Test
        void handleCreated_withNoBrand_setsBrandNameToNull() {
            // arrange
            Long productId = 1L;
            Long brandId = 10L;
            ProductEvents.Created event = new ProductEvents.Created(
                    productId,
                    brandId,
                    "Test Product",
                    BigDecimal.valueOf(10000),
                    ProductStatus.ON_SALE
            );

            when(brandRepository.findById(brandId)).thenReturn(Optional.empty());
            when(productViewRepository.save(any(ProductView.class))).thenReturn(Optional.of(ProductView.builder().build()));

            ConsumerRecord<String, ProductEvents.Created> record = createConsumerRecord("product.created.v1", event);

            // act
            consumer.handleCreated(record, acknowledgment);

            // assert
            verify(brandRepository).findById(brandId);
            verify(productViewRepository).save(argThat(pv ->
                    pv.getId().equals(productId) &&
                    pv.getBrandName() == null
            ));
            verify(acknowledgment).acknowledge();
        }
    }

    @DisplayName("handleUpdated 테스트")
    @Nested
    class HandleUpdatedTest {

        @DisplayName("성공 케이스: ProductView 업데이트")
        @Test
        void handleUpdated_updatesProductView() {
            // arrange
            Long productId = 1L;
            Long brandId = 10L;
            String brandName = "Updated Brand";
            ProductEvents.Updated event = new ProductEvents.Updated(
                    productId,
                    brandId,
                    "Updated Product",
                    BigDecimal.valueOf(12000),
                    ProductStatus.ON_SALE
            );

            Brand brand = Brand.builder()
                    .name(brandName)
                    .description("Test Description")
                    .status(com.loopers.domain.brand.BrandStatus.ON_SALE)
                    .isVisible(true)
                    .isSellable(true)
                    .build();

            when(brandRepository.findById(brandId)).thenReturn(Optional.of(brand));
            doNothing().when(productViewRepository).update(anyLong(), anyString(), any(), anyLong(), anyString(), any());

            ConsumerRecord<String, ProductEvents.Updated> record = createConsumerRecord("product.updated.v1", event);

            // act
            consumer.handleUpdated(record, acknowledgment);

            // assert
            verify(brandRepository).findById(brandId);
            verify(productViewRepository).update(
                    eq(productId),
                    eq("Updated Product"),
                    eq(BigDecimal.valueOf(12000)),
                    eq(brandId),
                    eq(brandName),
                    eq(ProductStatus.ON_SALE)
            );
            verify(acknowledgment).acknowledge();
        }

        @DisplayName("성공 케이스: Brand가 없는 경우 brandName은 null로 업데이트")
        @Test
        void handleUpdated_withNoBrand_setsBrandNameToNull() {
            // arrange
            Long productId = 1L;
            Long brandId = 10L;
            ProductEvents.Updated event = new ProductEvents.Updated(
                    productId,
                    brandId,
                    "Updated Product",
                    BigDecimal.valueOf(12000),
                    ProductStatus.ON_SALE
            );

            when(brandRepository.findById(brandId)).thenReturn(Optional.empty());
            doNothing().when(productViewRepository).update(anyLong(), anyString(), any(), anyLong(), anyString(), any());

            ConsumerRecord<String, ProductEvents.Updated> record = createConsumerRecord("product.updated.v1", event);

            // act
            consumer.handleUpdated(record, acknowledgment);

            // assert
            verify(brandRepository).findById(brandId);
            verify(productViewRepository).update(
                    eq(productId),
                    eq("Updated Product"),
                    eq(BigDecimal.valueOf(12000)),
                    eq(brandId),
                    isNull(),
                    eq(ProductStatus.ON_SALE)
            );
            verify(acknowledgment).acknowledge();
        }
    }

    @DisplayName("handleDeleted 테스트")
    @Nested
    class HandleDeletedTest {

        @DisplayName("성공 케이스: ProductView 삭제")
        @Test
        void handleDeleted_deletesProductView() {
            // arrange
            Long productId = 1L;
            ProductEvents.Deleted event = new ProductEvents.Deleted(
                    productId
            );

            doNothing().when(productViewRepository).deleteById(anyLong());

            ConsumerRecord<String, ProductEvents.Deleted> record = createConsumerRecord("product.deleted.v1", event);

            // act
            consumer.handleDeleted(record, acknowledgment);

            // assert
            verify(productViewRepository).deleteById(productId);
            verify(acknowledgment).acknowledge();
        }
    }
}

