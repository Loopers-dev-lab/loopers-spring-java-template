package com.loopers.domain.point;

import java.util.Optional;

public interface PointAccountRepository {

    Optional<PointAccount> find(String id);

}
