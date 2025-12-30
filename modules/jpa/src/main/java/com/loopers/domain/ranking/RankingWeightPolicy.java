package com.loopers.domain.ranking;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 랭킹 Weight Policy 엔티티
 * 이벤트 타입별 가중치를 동적으로 관리
 */
@Entity
@Table(name = "ranking_weight_policy", indexes = {
    @Index(name = "idx_event_type", columnList = "event_type", unique = true)
})
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class RankingWeightPolicy extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, unique = true, length = 20)
    private RankingEventType eventType;

    @Column(name = "weight", nullable = false)
    private Double weight;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Builder
    private RankingWeightPolicy(RankingEventType eventType, Double weight, Boolean isActive) {
        this.eventType = eventType;
        this.weight = weight;
        this.isActive = isActive != null ? isActive : true;
        this.guard();
    }

    public void updateWeight(Double newWeight) {
        if (newWeight == null || newWeight < 0) {
            throw new IllegalArgumentException("weight는 0 이상이어야 합니다");
        }
        this.weight = newWeight;
    }

    public void activate() {
        this.isActive = true;
    }

    public void deactivate() {
        this.isActive = false;
    }

    @Override
    protected void guard() {
        if (eventType == null) {
            throw new IllegalArgumentException("eventType은 필수입니다");
        }
        if (weight == null || weight < 0) {
            throw new IllegalArgumentException("weight는 0 이상이어야 합니다");
        }
        if (isActive == null) {
            this.isActive = true;
        }
    }
}

