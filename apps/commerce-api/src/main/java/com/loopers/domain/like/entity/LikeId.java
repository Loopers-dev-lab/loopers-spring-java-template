package com.loopers.domain.like.entity;

import java.io.Serializable;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@EqualsAndHashCode
@Builder
public class LikeId implements Serializable {
    
    private Long userId;
    private Long likeTargetId;

    @Enumerated(EnumType.STRING)
    private LikeTargetType likeTargetType;

}
