package com.loopers.application.members;

import com.loopers.domain.members.Member;
import com.loopers.domain.members.enums.Gender;

import java.time.LocalDate;

public record MemberInfo(
        Long id, 
        String memberId, 
        String email, 
        LocalDate birthDate, 
        Gender gender
) {
    public static MemberInfo from(Member model) {
        return new MemberInfo(
                model.getId(),
                model.getMemberId(),
                model.getEmail(),
                model.getBirthDate(),
                model.getGender()
        );
    }
}
