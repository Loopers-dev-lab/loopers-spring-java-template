package com.loopers.domain.ranking;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QWeeklyRankEntity is a Querydsl query type for WeeklyRankEntity
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QWeeklyRankEntity extends EntityPathBase<WeeklyRankEntity> {

    private static final long serialVersionUID = 2039637561L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QWeeklyRankEntity weeklyRankEntity = new QWeeklyRankEntity("weeklyRankEntity");

    public final DateTimePath<java.time.LocalDateTime> createdAt = createDateTime("createdAt", java.time.LocalDateTime.class);

    public final QWeeklyRankId id;

    public final NumberPath<Long> likeCount = createNumber("likeCount", Long.class);

    public final NumberPath<Long> orderCount = createNumber("orderCount", Long.class);

    public final NumberPath<Integer> rankPosition = createNumber("rankPosition", Integer.class);

    public final NumberPath<Long> salesCount = createNumber("salesCount", Long.class);

    public final NumberPath<Long> totalScore = createNumber("totalScore", Long.class);

    public final NumberPath<Long> viewCount = createNumber("viewCount", Long.class);

    public QWeeklyRankEntity(String variable) {
        this(WeeklyRankEntity.class, forVariable(variable), INITS);
    }

    public QWeeklyRankEntity(Path<? extends WeeklyRankEntity> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QWeeklyRankEntity(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QWeeklyRankEntity(PathMetadata metadata, PathInits inits) {
        this(WeeklyRankEntity.class, metadata, inits);
    }

    public QWeeklyRankEntity(Class<? extends WeeklyRankEntity> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.id = inits.isInitialized("id") ? new QWeeklyRankId(forProperty("id")) : null;
    }

}

