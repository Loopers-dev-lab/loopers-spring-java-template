package com.loopers.domain.ranking;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QWeeklyRankId is a Querydsl query type for WeeklyRankId
 */
@Generated("com.querydsl.codegen.DefaultEmbeddableSerializer")
public class QWeeklyRankId extends BeanPath<WeeklyRankId> {

    private static final long serialVersionUID = 1225730673L;

    public static final QWeeklyRankId weeklyRankId = new QWeeklyRankId("weeklyRankId");

    public final NumberPath<Long> productId = createNumber("productId", Long.class);

    public final StringPath yearWeek = createString("yearWeek");

    public QWeeklyRankId(String variable) {
        super(WeeklyRankId.class, forVariable(variable));
    }

    public QWeeklyRankId(Path<? extends WeeklyRankId> path) {
        super(path.getType(), path.getMetadata());
    }

    public QWeeklyRankId(PathMetadata metadata) {
        super(WeeklyRankId.class, metadata);
    }

}

