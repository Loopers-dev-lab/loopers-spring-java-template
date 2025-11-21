package com.loopers.domain.product;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QProduct is a Querydsl query type for Product
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QProduct extends EntityPathBase<Product> {

    private static final long serialVersionUID = 634840799L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QProduct product = new QProduct("product");

    public final com.loopers.domain.QBaseEntity _super = new com.loopers.domain.QBaseEntity(this);

    public final NumberPath<Long> brandId = createNumber("brandId", Long.class);

    //inherited
    public final DateTimePath<java.time.ZonedDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.ZonedDateTime> deletedAt = _super.deletedAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Long> likeCount = createNumber("likeCount", Long.class);

    public final StringPath name = createString("name");

    public final QMoney price;

    public final QStock stockQuantity;

    //inherited
    public final DateTimePath<java.time.ZonedDateTime> updatedAt = _super.updatedAt;

    public final NumberPath<Long> version = createNumber("version", Long.class);

    public QProduct(String variable) {
        this(Product.class, forVariable(variable), INITS);
    }

    public QProduct(Path<? extends Product> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QProduct(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QProduct(PathMetadata metadata, PathInits inits) {
        this(Product.class, metadata, inits);
    }

    public QProduct(Class<? extends Product> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.price = inits.isInitialized("price") ? new QMoney(forProperty("price")) : null;
        this.stockQuantity = inits.isInitialized("stockQuantity") ? new QStock(forProperty("stockQuantity")) : null;
    }

}

