package com.loopers.domain.order;

import com.loopers.domain.BaseEntity;
import com.loopers.domain.user.UserModel;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class OrderModel extends BaseEntity {

    private Integer orderCnt;
    private Character orderStatus;

    private Integer totalPrice;
    private Integer normalPrice;
    private Integer errorPrice;


    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserModel user;

    /**
     * ORDER ||--|{ ORDER_ITEM : "주문 내용" 은 양방향 관계로 설정합니다.
     */
    @OneToMany(mappedBy = "orders",
            cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private List<OrderItemModel> orderItems = new ArrayList<>();

}
