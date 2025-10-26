package com.loopers.core.infra.mysql.entity;

import jakarta.persistence.*;
import lombok.Getter;

import java.time.ZonedDateTime;

/**
 * 생성/수정/삭제 정보를 자동으로 관리해준다.
 * 재사용성을 위해 이 외의 컬럼이나 동작은 추가하지 않는다.
 */
@MappedSuperclass
@Getter
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private final Long id = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private ZonedDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private ZonedDateTime updatedAt;

    @Column(name = "deleted_at")
    private ZonedDateTime deletedAt;
}
