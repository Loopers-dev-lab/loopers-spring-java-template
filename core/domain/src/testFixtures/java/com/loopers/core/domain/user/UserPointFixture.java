package com.loopers.core.domain.user;

import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.domain.user.vo.UserPointBalance;
import com.loopers.core.domain.user.vo.UserPointId;
import org.instancio.Instancio;

import java.math.BigDecimal;

import static org.instancio.Select.field;

public class UserPointFixture {

    public static UserPoint create() {
        return Instancio.of(UserPoint.class)
                .set(field(UserPoint::getId), UserPointId.empty())
                .set(field(UserPoint::getUserId), Instancio.create(UserId.class))
                .set(field(UserPoint::getBalance), UserPointBalance.init())
                .create();
    }

    public static UserPoint createWith(UserId userId, BigDecimal balance) {
        return Instancio.of(UserPoint.class)
                .set(field(UserPoint::getId), UserPointId.empty())
                .set(field(UserPoint::getUserId), userId)
                .set(field(UserPoint::getBalance), new UserPointBalance(balance))
                .create();
    }
}
