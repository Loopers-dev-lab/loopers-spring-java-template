package com.loopers.domain.product;

import java.time.ZonedDateTime;
import java.util.Objects;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.brand.BrandEntity;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

/**
 * 상품 Materialized View 엔티티
 * <p>
 * 상품, 브랜드, 좋아요 정보를 통합하여 조회 성능을 최적화하기 위한 MV 테이블입니다.
 * 실시간 업데이트가 아닌 배치 업데이트(2분 간격)를 통해 데이터를 동기화합니다.
 *
 * @author hyunjikoh
 * @since 2025. 11. 27.
 */
@Entity
@Table(name = "product_materialized_view", indexes = {
        @Index(name = "idx_pmv_brand_id", columnList = "brand_id"),
        @Index(name = "idx_pmv_like_count", columnList = "like_count"),
        @Index(name = "idx_pmv_brand_like", columnList = "brand_id, like_count"),
        @Index(name = "idx_pmv_name", columnList = "name"),
        @Index(name = "idx_pmv_updated_at", columnList = "last_updated_at"),
        @Index(name = "idx_pmv_product_updated_at", columnList = "product_updated_at"),
        @Index(name = "idx_pmv_like_updated_at", columnList = "like_updated_at"),
        @Index(name = "idx_pmv_brand_updated_at", columnList = "brand_updated_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMaterializedViewEntity extends BaseEntity {

    @Column(name = "product_id", nullable = false, unique = true)
    private Long productId;

    // 상품 정보
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Embedded
    private Price price;

    @Column(name = "stock_quantity", nullable = false)
    private Integer stockQuantity;

    // 브랜드 정보
    @Column(name = "brand_id", nullable = false)
    private Long brandId;

    @Column(name = "brand_name", nullable = false, length = 100)
    private String brandName;

    // 좋아요 정보
    @Column(name = "like_count", nullable = false)
    private Long likeCount;

    /**
     * 상품명, 가격, 재고 등이 변경된 시간을 추적
     */
    @Column(name = "product_updated_at", nullable = false)
    private ZonedDateTime productUpdatedAt;

    /**
     * LikeEntity의 마지막 변경 시간
     * 좋아요 추가/삭제가 발생한 시간을 추적
     */
    @Column(name = "like_updated_at", nullable = false)
    private ZonedDateTime likeUpdatedAt;

    /**
     * 브랜드명, 설명 등이 변경된 시간을 추적
     */
    @Column(name = "brand_updated_at", nullable = false)
    private ZonedDateTime brandUpdatedAt;

    /**
     * MV의 마지막 동기화 시간
     * 배치 작업이 이 엔티티를 마지막으로 업데이트한 시간
     */
    @Column(name = "last_updated_at", nullable = false)
    private ZonedDateTime lastUpdatedAt;

    /**
     * ProductMaterializedViewEntity 생성자
     */
    private ProductMaterializedViewEntity(
            Long productId,
            String name,
            String description,
            Price price,
            Integer stockQuantity,
            Long brandId,
            String brandName,
            Long likeCount,
            ZonedDateTime productUpdatedAt,
            ZonedDateTime brandUpdatedAt,
            ZonedDateTime likeUpdatedAt,
            ZonedDateTime lastUpdatedAt

    ) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.brandId = brandId;
        this.brandName = brandName;
        this.likeCount = likeCount;
        this.productUpdatedAt = productUpdatedAt;
        this.brandUpdatedAt = brandUpdatedAt;
        this.likeUpdatedAt = likeUpdatedAt;
        this.lastUpdatedAt = lastUpdatedAt;
    }

    /**
     * Product, Brand, 좋아요 수로부터 MV 엔티티를 생성합니다.
     *
     * @param product       상품 엔티티
     * @param brand         브랜드 엔티티
     * @param likeCount     좋아요 수
     * @param likeUpdatedAt 좋아요 최신 업데이트 시간
     * @return 생성된 MV 엔티티
     * @throws IllegalArgumentException 필수 파라미터가 null인 경우
     */
    public static ProductMaterializedViewEntity from(
            ProductEntity product,
            BrandEntity brand,
            Long likeCount,
            ZonedDateTime likeUpdatedAt
    ) {
        Objects.requireNonNull(product, "상품 엔티티는 필수입니다.");
        Objects.requireNonNull(brand, "브랜드 엔티티는 필수입니다.");
        Objects.requireNonNull(likeCount, "좋아요 수는 필수입니다.");

        if (likeCount < 0) {
            throw new IllegalArgumentException("좋아요 수는 0 이상이어야 합니다.");
        }

        return new ProductMaterializedViewEntity(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStockQuantity(),
                brand.getId(),
                brand.getName(),
                likeCount,
                product.getUpdatedAt(),
                brand.getUpdatedAt(),
                likeUpdatedAt != null ? likeUpdatedAt : ZonedDateTime.now(),
                ZonedDateTime.now()
        );
    }

    /**
     * ProductMVSyncDto로부터 MV 엔티티를 생성합니다.
     * <p>
     * 배치 동기화 시 단일 쿼리 결과로부터 MV를 생성합니다.
     *
     * @param dto 동기화용 DTO
     * @return 생성된 MV 엔티티
     * @throws IllegalArgumentException 필수 파라미터가 null인 경우
     */
    public static ProductMaterializedViewEntity fromDto(ProductMVSyncDto dto) {
        Objects.requireNonNull(dto, "동기화 DTO는 필수입니다.");

        Long likeCount = dto.getLikeCount() != null ? dto.getLikeCount() : 0L;
        if (likeCount < 0) {
            throw new IllegalArgumentException("좋아요 수는 0 이상이어야 합니다.");
        }

        return new ProductMaterializedViewEntity(
                dto.getProductId(),
                dto.getProductName(),
                dto.getProductDescription(),
                Price.of(dto.getOriginPrice(), dto.getDiscountPrice()),
                dto.getStockQuantity(),
                dto.getBrandId(),
                dto.getBrandName(),
                likeCount,
                dto.getProductUpdatedAt(),
                dto.getBrandUpdatedAt(),
                dto.getLikeUpdatedAt() != null ? dto.getLikeUpdatedAt() : ZonedDateTime.now(),
                ZonedDateTime.now()
        );
    }

    /**
     * 기존 MV 엔티티를 최신 데이터로 동기화합니다.
     *
     * @param product   최신 상품 엔티티
     * @param brand     최신 브랜드 엔티티
     * @param likeCount 최신 좋아요 수
     * @throws IllegalArgumentException 필수 파라미터가 null인 경우
     */
    public void sync(ProductEntity product, BrandEntity brand, Long likeCount) {
        Objects.requireNonNull(product, "상품 엔티티는 필수입니다.");
        Objects.requireNonNull(brand, "브랜드 엔티티는 필수입니다.");
        Objects.requireNonNull(likeCount, "좋아요 수는 필수입니다.");

        if (likeCount < 0) {
            throw new IllegalArgumentException("좋아요 수는 0 이상이어야 합니다.");
        }

        this.name = product.getName();
        this.description = product.getDescription();
        this.price = product.getPrice();
        this.stockQuantity = product.getStockQuantity();
        this.brandId = brand.getId();
        this.brandName = brand.getName();
        this.likeCount = likeCount;
        this.productUpdatedAt = product.getUpdatedAt();
        this.brandUpdatedAt = brand.getUpdatedAt();
        this.lastUpdatedAt = ZonedDateTime.now();
    }

    /**
     * DTO로부터 기존 MV 엔티티를 동기화합니다.
     * <p>
     * 배치 동기화 시 엔티티를 새로 생성하지 않고 필드만 업데이트합니다.
     *
     * @param dto 동기화용 DTO
     * @throws IllegalArgumentException 필수 파라미터가 null인 경우
     */
    public void syncFromDto(ProductMVSyncDto dto) {
        Objects.requireNonNull(dto, "동기화 DTO는 필수입니다.");

        Long likeCount = dto.getLikeCount() != null ? dto.getLikeCount() : 0L;
        if (likeCount < 0) {
            throw new IllegalArgumentException("좋아요 수는 0 이상이어야 합니다.");
        }

        this.name = dto.getProductName();
        this.description = dto.getProductDescription();
        this.price = Price.of(dto.getOriginPrice(), dto.getDiscountPrice());
        this.stockQuantity = dto.getStockQuantity();
        this.productId = dto.getProductId();
        this.brandId = dto.getBrandId();
        this.brandName = dto.getBrandName();
        this.likeCount = likeCount;
        this.productUpdatedAt = dto.getProductUpdatedAt();
        this.brandUpdatedAt = dto.getBrandUpdatedAt();
        this.likeUpdatedAt = dto.getLikeUpdatedAt() != null ? dto.getLikeUpdatedAt() : ZonedDateTime.now();
        this.lastUpdatedAt = ZonedDateTime.now();
    }

    @Override
    protected void guard() {
        if (this.productId == null) {
            throw new IllegalStateException("상품 ID는 필수입니다.");
        }

        if (this.name == null || this.name.isBlank()) {
            throw new IllegalStateException("상품명은 비어있을 수 없습니다.");
        }

        if (this.name.length() > 200) {
            throw new IllegalStateException("상품명은 200자를 초과할 수 없습니다.");
        }

        if (this.price == null) {
            throw new IllegalStateException("가격 정보는 필수입니다.");
        }

        if (this.stockQuantity == null || this.stockQuantity < 0) {
            throw new IllegalStateException("재고 수량은 0 이상이어야 합니다.");
        }

        if (this.brandId == null) {
            throw new IllegalStateException("브랜드 ID는 필수입니다.");
        }

        if (this.brandName == null || this.brandName.isBlank()) {
            throw new IllegalStateException("브랜드명은 비어있을 수 없습니다.");
        }

        if (this.brandName.length() > 100) {
            throw new IllegalStateException("브랜드명은 100자를 초과할 수 없습니다.");
        }

        if (this.likeCount == null || this.likeCount < 0) {
            throw new IllegalStateException("좋아요 수는 0 이상이어야 합니다.");
        }

        if (this.lastUpdatedAt == null) {
            throw new IllegalStateException("마지막 업데이트 시간은 필수입니다.");
        }
    }


    /**
     * DTO와 기존 MV를 비교하여 실제 변경사항이 있는지 확인합니다.
     */
    public static boolean hasActualChangesFromDto(ProductMaterializedViewEntity mv, ProductMVSyncDto dto) {
        // 상품명 변경
        if (!mv.getName().equals(dto.getProductName())) {
            return true;
        }

        if (!Objects.equals(mv.getDescription(), dto.getProductDescription())) {
            return true;
        }

        // 가격 변경
        if (!mv.getPrice().getOriginPrice().equals(dto.getOriginPrice())) {
            return true;
        }

        // 할인 가격 변경
        if (dto.getDiscountPrice() != null && !mv.getPrice().getDiscountPrice().equals(dto.getDiscountPrice())) {
            return true;
        }

        // 재고 변경
        if (!mv.getStockQuantity().equals(dto.getStockQuantity())) {
            return true;
        }

        // 브랜드명 변경
        if (!mv.getBrandName().equals(dto.getBrandName())) {
            return true;
        }

        // 좋아요 수 변경
        Long dtoLikeCount = dto.getLikeCount() != null ? dto.getLikeCount() : 0L;
        if (!mv.getLikeCount().equals(dtoLikeCount)) {
            return true;
        }

        return false;
    }
}
