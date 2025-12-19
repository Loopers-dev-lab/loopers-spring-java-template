package com.loopers.domain.members;

import com.loopers.domain.members.repository.MemberRepository;
import com.loopers.support.TestEntityUtils;

import java.util.HashMap;
import java.util.Map;

public class InMemoryMemberRepository implements MemberRepository {

    private final Map<String, Member> store = new HashMap<>();
    private long sequence = 0L;

    @Override
    public Member save(Member member) {
        if (member.getId() == null) {
            member = TestEntityUtils.setIdWithNow(member, ++sequence);
        }
        store.put(member.getMemberId(), member);
        return member;
    }

    @Override
    public Member findByMemberId(String memberId) {
        return store.get(memberId);
    }

    @Override
    public boolean existsByMemberId(String memberId) {
        return store.containsKey(memberId);
    }

    @Override
    public boolean existsById(Long id) {
        return store.values().stream()
                .anyMatch(member -> member.getId().equals(id));
    }

    public void clear() {
        store.clear();
        sequence = 0L;
    }
}
