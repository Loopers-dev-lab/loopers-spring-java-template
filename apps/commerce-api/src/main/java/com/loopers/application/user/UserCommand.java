package com.loopers.application.user;

public class UserCommand {

    public record SignupCommand(
            String loginId,
            String email,
            String birthDate,
            String gender
    ) {
    }
}
