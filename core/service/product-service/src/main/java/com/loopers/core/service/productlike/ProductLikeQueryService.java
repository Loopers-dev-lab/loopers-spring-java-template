package com.loopers.core.service.productlike;

import com.loopers.core.domain.brand.vo.BrandId;
import com.loopers.core.domain.common.type.OrderSort;
import com.loopers.core.domain.productlike.LikeProductListView;
import com.loopers.core.domain.productlike.repository.ProductLikeRepository;
import com.loopers.core.domain.user.User;
import com.loopers.core.domain.user.repository.UserRepository;
import com.loopers.core.domain.user.vo.UserIdentifier;
import com.loopers.core.service.productlike.query.GetLikeProductsListQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductLikeQueryService {

    private final ProductLikeRepository repository;
    private final UserRepository userRepository;

    public LikeProductListView getLikeProductsListView(GetLikeProductsListQuery query) {
        User user = userRepository.getByIdentifier(new UserIdentifier(query.getUserIdentifier()));

        return repository.findLikeProductsListWithCondition(
                user.getId(),
                new BrandId(query.getBrandId()),
                OrderSort.from(query.getCreatedAtSort()),
                OrderSort.from(query.getPriceSort()),
                OrderSort.from(query.getLikeCountSort()),
                query.getPageNo(),
                query.getPageSize()
        );
    }
}
