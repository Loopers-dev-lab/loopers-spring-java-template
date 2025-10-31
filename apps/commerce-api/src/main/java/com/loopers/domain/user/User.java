package com.loopers.domain.user;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.regex.Pattern;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "users")
public class User {

    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]{1,10}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern BIRTHDATE_PATTERN = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$"); // yyyy-MM-dd

    private static final String INVALID_ID_MESSAGE = "ID는 영문 및 숫자 10자 이내여야 합니다.";
    private static final String INVALID_EMAIL_MESSAGE = "이메일은 xx@yy.zz 형식이어야 합니다.";
    private static final String INVALID_BIRTHDATE_MESSAGE = "생년월일은 yyyy-MM-dd 형식이어야 합니다.";
    private static final String INVALID_GENDER_MESSAGE = "성별은 MALE 또는 FEMALE이어야 합니다.";

    @Id
    private String id;
    private String email;
    private String birthDate;
    private String gender;

    /**
     * Create a User from the provided attributes after validating each value.
     *
     * @param id        user identifier; 1–10 alphanumeric characters
     * @param email     user email address in the form `local@domain.tld`
     * @param birthDate birth date in `yyyy-MM-dd` format
     * @param gender    user gender; either {@code "MALE"} or {@code "FEMALE"}
     * @return          a new User instance with the given attributes
     */
    public static User create(String id, String email, String birthDate, String gender) {
        validateId(id);
        validateEmail(email);
        validateBirthDate(birthDate);
        validateGender(gender);
        return new User(id, email, birthDate, gender);
    }

    /**
     * Validates that the user id consists of 1 to 10 alphanumeric characters.
     *
     * @param id the user identifier to validate
     * @throws IllegalArgumentException if id is null or does not match the required format
     */
    private static void validateId(String id) {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException(INVALID_ID_MESSAGE);
        }
    }

    /**
     * Validates that the provided email is non-null and matches the expected email format.
     *
     * @param email the email address to validate (expected format: `xx@yy.zz`)
     * @throws IllegalArgumentException if {@code email} is null or does not match the required format
     */
    private static void validateEmail(String email) {
        if (email == null || !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException(INVALID_EMAIL_MESSAGE);
        }
    }

    /**
     * Validates that the birth date is provided and matches the yyyy-MM-dd format.
     *
     * @param birthDate the birth date string to validate (expected format: yyyy-MM-dd)
     * @throws IllegalArgumentException if {@code birthDate} is null or does not match the yyyy-MM-dd pattern
     */
    private static void validateBirthDate(String birthDate) {
        if (birthDate == null || !BIRTHDATE_PATTERN.matcher(birthDate).matches()) {
            throw new IllegalArgumentException(INVALID_BIRTHDATE_MESSAGE);
        }
    }

    /**
     * Ensures the provided gender is either "MALE" or "FEMALE".
     *
     * @param gender the gender value to validate; expected exact values "MALE" or "FEMALE"
     * @throws IllegalArgumentException if {@code gender} is null or not "MALE" or "FEMALE"
     */
    private static void validateGender(String gender) {
        if (gender == null || (!gender.equals("MALE") && !gender.equals("FEMALE"))) {
            throw new IllegalArgumentException(INVALID_GENDER_MESSAGE);
        }
    }
}