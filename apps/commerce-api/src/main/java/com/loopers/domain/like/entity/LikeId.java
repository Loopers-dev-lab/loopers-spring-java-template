package com.loopers.domain.like.entity;

import java.io.Serializable;

import jakarta.persistence.Column;
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
    
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "like_target_id")
    private Long likeTargetId;

    @Enumerated(EnumType.STRING)
    @Column(name = "like_target_type")
    private LikeTargetType likeTargetType;

}
