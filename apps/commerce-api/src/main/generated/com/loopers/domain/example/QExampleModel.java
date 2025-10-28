package com.loopers.domain.example;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QExampleModel is a Querydsl query type for ExampleModel
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QExampleModel extends EntityPathBase<ExampleModel> {

    private static final long serialVersionUID = 1117848404L;

    public static final QExampleModel exampleModel = new QExampleModel("exampleModel");

    public final com.loopers.domain.QBaseEntity _super = new com.loopers.domain.QBaseEntity(this);

    //inherited
    public final DateTimePath<java.time.ZonedDateTime> createdAt = _super.createdAt;

    //inherited
    public final DateTimePath<java.time.ZonedDateTime> deletedAt = _super.deletedAt;

    public final StringPath description = createString("description");

    //inherited
    public final NumberPath<Long> id = _super.id;

    public final StringPath name = createString("name");

    //inherited
    public final DateTimePath<java.time.ZonedDateTime> updatedAt = _super.updatedAt;

    public QExampleModel(String variable) {
        super(ExampleModel.class, forVariable(variable));
    }

    public QExampleModel(Path<? extends ExampleModel> path) {
        super(path.getType(), path.getMetadata());
    }

    public QExampleModel(PathMetadata metadata) {
        super(ExampleModel.class, metadata);
    }

}

