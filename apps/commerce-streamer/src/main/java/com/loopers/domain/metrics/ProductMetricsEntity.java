package com.loopers.domain.metrics;

import java.time.ZonedDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 *
 * @author hyunjikoh
 * @since 2025. 12. 16.
 */

@Entity
@Getter
@Table(name = "product_metrics")
@AllArgsConstructor
@NoArgsConstructor
public class ProductMetricsEntity {
    @Id
    @Column(name = "product_id", nullable = false)
    private Long id;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    @Column(name = "like_count", nullable = false)
    private long likeCount;

    @Column(name = "sales_count", nullable = false)
    private long salesCount;

    @Column(name = "last_event_at")
    private ZonedDateTime lastEventAt;


}
