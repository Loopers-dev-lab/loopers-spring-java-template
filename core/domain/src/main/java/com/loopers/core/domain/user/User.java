package com.loopers.core.domain.user;

import com.loopers.core.domain.common.vo.CreatedAt;
import com.loopers.core.domain.common.vo.DeletedAt;
import com.loopers.core.domain.common.vo.UpdatedAt;
import com.loopers.core.domain.user.type.UserGender;
import com.loopers.core.domain.user.vo.UserBirthDay;
import com.loopers.core.domain.user.vo.UserEmail;
import com.loopers.core.domain.user.vo.UserId;
import com.loopers.core.domain.user.vo.UserIdentifier;
import lombok.Getter;

@Getter
public class User {

    private final UserId userId;

    private final UserIdentifier identifier;

    private final UserEmail email;

    private final UserBirthDay birthDay;

    private final UserGender gender;

    private final CreatedAt createdAt;

    private final UpdatedAt updatedAt;

    private final DeletedAt deletedAt;

    private User(
            UserId userId,
            UserIdentifier identifier,
            UserEmail email,
            UserBirthDay birthDay,
            UserGender gender,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        this.userId = userId;
        this.identifier = identifier;
        this.email = email;
        this.birthDay = birthDay;
        this.gender = gender;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.deletedAt = deletedAt;
    }

    public static User create(
            UserIdentifier identifier,
            UserEmail email,
            UserBirthDay birthDay,
            UserGender gender
    ) {
        return new User(
                UserId.empty(),
                identifier,
                email,
                birthDay,
                gender,
                CreatedAt.now(),
                UpdatedAt.now(),
                DeletedAt.empty()
        );
    }

    public static User mappedBy(
            UserId userId,
            UserIdentifier identifier,
            UserEmail email,
            UserBirthDay birthDay,
            UserGender gender,
            CreatedAt createdAt,
            UpdatedAt updatedAt,
            DeletedAt deletedAt
    ) {
        return new User(userId, identifier, email, birthDay, gender, createdAt, updatedAt, deletedAt);
    }
}
