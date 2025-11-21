package com.loopers.domain.members.service;

import com.loopers.domain.members.Member;
import com.loopers.domain.members.enums.Gender;
import com.loopers.domain.members.repository.MemberRepository;
import com.loopers.support.error.CoreException;
import com.loopers.support.error.ErrorType;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Component
public class MemberService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public Member registerMember(String memberId, String email, String password, String birthDate, Gender gender) {
        String encodedPassword = passwordEncoder.encode(password);
        Member member = new Member(memberId, email, encodedPassword, birthDate, gender);

        try {
            return memberRepository.save(member);
        } catch (DataIntegrityViolationException e) {
            throw new CoreException(
                    ErrorType.CONFLICT,
                    "이미 가입된 ID 입니다."
            );
        }
    }

    @Transactional(readOnly = true)
    public Member getMemberByMemberId(String memberId) {
        return memberRepository.findByMemberId(memberId);
    }
}
