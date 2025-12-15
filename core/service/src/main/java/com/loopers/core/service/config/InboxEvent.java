package com.loopers.core.service.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface InboxEvent {
    String aggregateType();

    String eventType();

    String eventIdField();

    String aggregateIdField() default "id";
}
