package com.loopers.domain.order.service;

import com.loopers.domain.common.vo.Money;
import com.loopers.domain.coupon.MemberCoupon;
import com.loopers.domain.coupon.repository.MemberCouponRepository;
import com.loopers.domain.members.repository.MemberRepository;
import com.loopers.domain.order.Order;
import com.loopers.domain.order.OrderItem;
import com.loopers.domain.order.command.OrderLineCommand;
import com.loopers.domain.order.command.OrderPlacementCommand;
import com.loopers.domain.order.repository.OrderRepository;
import com.loopers.domain.product.Product;
import com.loopers.domain.product.repository.ProductRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Component
public class OrderPlacementService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final MemberCouponRepository memberCouponRepository;

    public Order placeOrder(OrderPlacementCommand command) {
        validateMemberExists(command.getMemberId());

        MemberCoupon memberCoupon = null;
        if (command.hasCoupon()) {
            memberCoupon = validateAndLockCoupon(command.getMemberCouponId(), command.getMemberId());
        }

        List<OrderItem> items = processOrderLines(command.getOrderLines());
        Money totalPrice = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(Money.zero(), Money::plus);

        Money discountAmount = Money.zero();
        if (memberCoupon != null) {
            discountAmount = memberCoupon.calculateDiscount(totalPrice);
        }

        Money finalPrice = totalPrice.minus(discountAmount);
        if (finalPrice.isLessThan(Money.zero())) {
            finalPrice = Money.zero();
        }

        // 쿠폰 사용 처리 (pessimistic lock 내에서 수행하여 동시성 보장)
        if (memberCoupon != null) {
            memberCoupon.use();
            memberCouponRepository.save(memberCoupon);
        }

        Order order = Order.create(command.getMemberId(), items, finalPrice);
        return orderRepository.save(order);
    }

    private MemberCoupon validateAndLockCoupon(Long memberCouponId, Long memberId) {
        MemberCoupon memberCoupon = memberCouponRepository.findByIdForUpdate(memberCouponId)
                .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "쿠폰을 찾을 수 없습니다."));

        memberCoupon.validateOwnership(memberId);
        memberCoupon.validateUsable();

        return memberCoupon;
    }

    private List<OrderItem> processOrderLines(List<OrderLineCommand> orderLines) {
        List<OrderItem> items = new ArrayList<>();

        for (OrderLineCommand line : orderLines) {
            Product product = productRepository.findById(line.getProductId())
                    .orElseThrow(() -> new CoreException(ErrorType.NOT_FOUND, "상품을 찾을 수 없습니다."));

            int updatedRows = productRepository.decreaseStock(product.getId(), line.getQuantity());
            if (updatedRows == 0) {
                throw new CoreException(ErrorType.BAD_REQUEST, "재고가 부족합니다.");
            }

            items.add(new OrderItem(product.getId(), line.getQuantity(), product.getPrice()));
        }

        return items;
    }

    private void validateMemberExists(Long memberId) {
        if (!memberRepository.existsById(memberId)) {
            throw new CoreException(ErrorType.NOT_FOUND, "회원을 찾을 수 없습니다.");
        }
    }
}
