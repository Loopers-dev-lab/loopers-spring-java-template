package com.loopers.infrastructure.product;

import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.domain.product.command.ProductSearchFilter;
import com.loopers.domain.product.enums.ProductSortCondition;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

import static com.loopers.domain.product.QProduct.product;

@RequiredArgsConstructor
@Repository
public class ProductRepositoryImpl implements ProductRepository {
    
    private final ProductJpaRepository productJpaRepository;
    private final JPAQueryFactory queryFactory;

    @Override
    public Optional<Product> findById(Long id) {
        return productJpaRepository.findById(id);
    }

    @Override
    public Optional<Product> findByIdForUpdate(Long id) {
        return productJpaRepository.findByIdForUpdate(id);
    }

    @Override
    public List<Product> findByIdIn(List<Long> ids) {
        return productJpaRepository.findAllById(ids);
    }

    @Override
    public Page<Product> findAll(ProductSearchFilter filter, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();

        if (filter.getBrandId() != null) {
            builder.and(product.brandId.eq(filter.getBrandId()));
        }

        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            builder.and(product.name.containsIgnoreCase(filter.getKeyword())
                    .or(product.description.containsIgnoreCase(filter.getKeyword())));
        }

        JPAQuery<Product> query = queryFactory
                .selectFrom(product)
                .where(builder);

        applySorting(query, filter.getSortCondition());

        // Fetch one more item to check if there are more pages
        List<Product> results = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize() + 1)
                .fetch();

        List<Product> content;
        long total;
        
        if (results.size() <= pageable.getPageSize()) {
            // Last page or exact page size
            content = results;
            total = pageable.getOffset() + results.size();
        } else {
            // There are more items, remove the extra one
            content = results.subList(0, pageable.getPageSize());
            // Only count when we know there are more pages
            total = queryFactory.select(product.count()).from(product).where(builder).fetchOne();
        }

        return new PageImpl<>(content, pageable, total);
    }

    @Override
    public List<Product> findAllByCursor(ProductSearchFilter filter, String cursor, int size) {
        BooleanBuilder builder = new BooleanBuilder();

        // Apply filters
        if (filter.getBrandId() != null) {
            builder.and(product.brandId.eq(filter.getBrandId()));
        }

        if (filter.getKeyword() != null && !filter.getKeyword().trim().isEmpty()) {
            builder.and(product.name.containsIgnoreCase(filter.getKeyword())
                    .or(product.description.containsIgnoreCase(filter.getKeyword())));
        }

        // Apply cursor condition
        if (cursor != null && !cursor.trim().isEmpty()) {
            applyCursorCondition(builder, cursor, filter.getSortCondition());
        }

        JPAQuery<Product> query = queryFactory
                .selectFrom(product)
                .where(builder);

        applyCursorSorting(query, filter.getSortCondition());

        // Fetch size + 1 to check if there are more items
        return query.limit(size + 1).fetch();
    }

    @Override
    public Product save(Product product) {
        return productJpaRepository.save(product);
    }

    @Override
    public int decreaseStock(Long productId, int quantity) {
        return Math.toIntExact(queryFactory
                .update(product)
                .set(product.stock.quantity, product.stock.quantity.subtract(quantity))
                .where(product.id.eq(productId)
                        .and(product.stock.quantity.goe(quantity)))
                .execute());
    }

    @Override
    public int increaseStock(Long productId, int quantity) {
        return Math.toIntExact(queryFactory
                .update(product)
                .set(product.stock.quantity, product.stock.quantity.add(quantity))
                .where(product.id.eq(productId))
                .execute());
    }

    @Override
    public int getStockQuantity(Long productId) {
        Integer stockQuantity = queryFactory
                .select(product.stock.quantity)
                .from(product)
                .where(product.id.eq(productId))
                .fetchOne();
        return stockQuantity != null ? stockQuantity : 0;
    }

    @Override
    public int incrementLikeCount(Long productId) {
        return Math.toIntExact(queryFactory
                .update(product)
                .set(product.likeCount, product.likeCount.add(1))
                .where(product.id.eq(productId))
                .execute());
    }

    @Override
    public int decrementLikeCount(Long productId) {
        return Math.toIntExact(queryFactory
                .update(product)
                .set(product.likeCount, product.likeCount.subtract(1))
                .where(product.id.eq(productId)
                        .and(product.likeCount.gt(0)))
                .execute());
    }

    @Override
    public int updateLikeCount(Long productId, long count) {
        return Math.toIntExact(queryFactory
                .update(product)
                .set(product.likeCount, (int) count)
                .where(product.id.eq(productId))
                .execute());
    }

    @Override
    public List<Product> findTopByLikeCount(int limit) {
        return queryFactory
                .selectFrom(product)
                .orderBy(product.likeCount.desc(), product.id.desc())
                .limit(limit)
                .fetch();
    }

    @Override
    public List<Product> findTopByBrandIdAndLikeCount(Long brandId, int limit) {
        return queryFactory
                .selectFrom(product)
                .where(product.brandId.eq(brandId))
                .orderBy(product.likeCount.desc(), product.id.desc())
                .limit(limit)
                .fetch();
    }

    private void applySorting(JPAQuery<Product> query, ProductSortCondition sortCondition) {
        if (sortCondition == null) {
            query.orderBy(product.createdAt.desc());
            return;
        }

        switch (sortCondition) {
            case LATEST -> query.orderBy(product.createdAt.desc());
            case PRICE_ASC -> query.orderBy(product.price.amount.asc());
            case LIKES_DESC -> query.orderBy(product.likeCount.desc());
        }
    }

    private void applyCursorSorting(JPAQuery<Product> query, ProductSortCondition sortCondition) {
        if (sortCondition == null || sortCondition == ProductSortCondition.LATEST) {
            query.orderBy(product.createdAt.desc(), product.id.desc());
        } else if (sortCondition == ProductSortCondition.PRICE_ASC) {
            query.orderBy(product.price.amount.asc(), product.id.asc());
        } else if (sortCondition == ProductSortCondition.LIKES_DESC) {
            query.orderBy(product.likeCount.desc(), product.id.desc());
        }
    }

    private void applyCursorCondition(BooleanBuilder builder, String cursor, ProductSortCondition sortCondition) {
        String[] parts = decodeCursor(cursor);
        if (parts == null) {
            return;
        }

        if (sortCondition == null || sortCondition == ProductSortCondition.LATEST) {
            // cursor format: "createdAt,id"
            ZonedDateTime createdAt = ZonedDateTime.parse(parts[0]);
            Long id = Long.parseLong(parts[1]);
            builder.and(
                    product.createdAt.lt(createdAt)
                            .or(product.createdAt.eq(createdAt).and(product.id.lt(id)))
            );
        } else if (sortCondition == ProductSortCondition.PRICE_ASC) {
            // cursor format: "price,id"
            BigDecimal price = new BigDecimal(parts[0]);
            Long id = Long.parseLong(parts[1]);
            builder.and(
                    product.price.amount.gt(price)
                            .or(product.price.amount.eq(price).and(product.id.gt(id)))
            );
        } else if (sortCondition == ProductSortCondition.LIKES_DESC) {
            // cursor format: "likeCount,id"
            int likeCount = Integer.parseInt(parts[0]);
            Long id = Long.parseLong(parts[1]);
            builder.and(
                    product.likeCount.lt(likeCount)
                            .or(product.likeCount.eq(likeCount).and(product.id.lt(id)))
            );
        }
    }

    public static String encodeCursor(Product product, ProductSortCondition sortCondition) {
        String cursorValue;
        if (sortCondition == null || sortCondition == ProductSortCondition.LATEST) {
            cursorValue = product.getCreatedAt() + "," + product.getId();
        } else if (sortCondition == ProductSortCondition.PRICE_ASC) {
            cursorValue = product.getPrice().getAmount() + "," + product.getId();
        } else if (sortCondition == ProductSortCondition.LIKES_DESC) {
            cursorValue = product.getLikeCount() + "," + product.getId();
        } else {
            cursorValue = product.getId().toString();
        }
        return Base64.getEncoder().encodeToString(cursorValue.getBytes());
    }

    private String[] decodeCursor(String cursor) {
        try {
            String decoded = new String(Base64.getDecoder().decode(cursor));
            return decoded.split(",");
        } catch (Exception e) {
            return null;
        }
    }
}
