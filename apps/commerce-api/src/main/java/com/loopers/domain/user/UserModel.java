package com.loopers.domain.user;

import com.loopers.domain.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "user")
public class UserModel extends BaseEntity {

    private UserId userId;
    private Email email;
    private Gender gender;
    private BirthDate birthDate;

    protected UserModel() {}

    public UserModel(UserId userId, Email email, Gender gender, BirthDate birthDate) {
        this.userId = userId;
        this.email = email;
        this.gender = gender;
        this.birthDate = birthDate;
    }

    public UserId getUserId() {
        return userId;
    }

    public Email getEmail() {
        return email;
    }

    public Gender getGender() {
        return gender;
    }

    public BirthDate getBirthDate() {
        return birthDate;
    }
}
