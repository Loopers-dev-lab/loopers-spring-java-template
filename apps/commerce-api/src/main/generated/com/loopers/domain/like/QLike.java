package com.loopers.domain.like;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QLike is a Querydsl query type for Like
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QLike extends EntityPathBase<Like> {

    private static final long serialVersionUID = 906743549L;

    public static final QLike like = new QLike("like1");

    public final com.loopers.domain.QBaseEntity _super = new com.loopers.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.ZonedDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.ZonedDateTime> deletedAt = _super.deletedAt;

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final NumberPath<Long> productId = createNumber("productId", Long.class);

    //inherited
    public final DateTimePath<java.time.ZonedDateTime> updatedAt = _super.updatedAt;

    public final StringPath userId = createString("userId");

    public QLike(String variable) {
        super(Like.class, forVariable(variable));
    }

    public QLike(Path<? extends Like> path) {
        super(path.getType(), path.getMetadata());
    }

    public QLike(PathMetadata metadata) {
        super(Like.class, metadata);
    }

}

