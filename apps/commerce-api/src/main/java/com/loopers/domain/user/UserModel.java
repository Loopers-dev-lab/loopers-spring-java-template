package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Embedded;

@Entity
@Table(name = "user")
public class UserModel extends BaseEntity {

    @Embedded
    private UserId userId;

    @Embedded
    private Email email;

    @Embedded
    private Gender gender;

    @Embedded
    private BirthDate birthDate;

    protected UserModel() {
    }

    public UserModel(UserId userId, Email email, Gender gender, BirthDate birthDate) {
        this.userId = userId;
        this.email = email;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    public UserId getUserId() {
        return this.userId;
    }

    public Email getEmail() {
        return this.email;
    }

    public Gender getGender() {
        return this.gender;
    }

    public BirthDate getBirthDate() {
        return this.birthDate;
    }
}