package com.loopers.domain.members;

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
    public MemberModel registerMember(String memberId, String email, String password, String birthDate, Gender gender) {
        String encodedPassword = passwordEncoder.encode(password);
        MemberModel member = new MemberModel(memberId, email, encodedPassword, birthDate, gender);

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
    public MemberModel getMemberByMemberId(String memberId) {
        return memberRepository.findByMemberId(memberId);
    }
}
