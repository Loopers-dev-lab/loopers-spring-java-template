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
    public List<Product> findByIdIn(List<Long> ids) {
        return productJpaRepository.findAllById(ids);
    }

    @Override
    public Page<Product> findAll(ProductSearchFilter filter, Pageable pageable) {
        BooleanBuilder builder = new BooleanBuilder();
        
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
            total = queryFactory.selectFrom(product).where(builder).fetchCount();
        }

        return new PageImpl<>(content, pageable, total);
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
}