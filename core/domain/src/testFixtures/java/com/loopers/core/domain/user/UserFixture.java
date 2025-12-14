package com.loopers.core.domain.user;

import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.domain.user.vo.UserIdentifier;
import org.instancio.Instancio;

import static org.instancio.Select.field;

public class UserFixture {

    public static User create() {
        return Instancio.of(User.class)
                .set(field(User::getId), UserId.empty())
                .set(field(User::getIdentifier), UserIdentifier.create("testuser"))
                .set(field(User::getEmail), UserEmail.create("test@example.com"))
                .set(field(User::getBirthDay), UserBirthDay.create("1990-01-01"))
                .set(field(User::getGender), UserGender.create("MALE"))
                .create();
    }

    public static User createWith(String identifier) {
        return Instancio.of(User.class)
                .set(field(User::getId), UserId.empty())
                .set(field(User::getIdentifier), UserIdentifier.create(identifier))
                .set(field(User::getEmail), UserEmail.create("test@example.com"))
                .set(field(User::getBirthDay), UserBirthDay.create("1990-01-01"))
                .set(field(User::getGender), UserGender.create("MALE"))
                .create();
    }

    public static User createWith(String identifier, String email) {
        return Instancio.of(User.class)
                .set(field(User::getId), UserId.empty())
                .set(field(User::getIdentifier), UserIdentifier.create(identifier))
                .set(field(User::getEmail), UserEmail.create(email))
                .set(field(User::getBirthDay), UserBirthDay.create("1990-01-01"))
                .set(field(User::getGender), UserGender.create("MALE"))
                .create();
    }
}
