package com.badargadh.sahkar.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.badargadh.sahkar.data.MemberHistory;

@Repository
public interface MemberHistoryRepository extends JpaRepository<MemberHistory, Long> {
    // Search history by member number (will show all previous owners)
    List<MemberHistory> findAllByMemberNo(Integer memberNo);
    
    MemberHistory findByOriginalMemberId(Long memberId);
}