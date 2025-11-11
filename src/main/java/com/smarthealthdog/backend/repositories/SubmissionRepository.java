package com.smarthealthdog.backend.repositories;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.smarthealthdog.backend.domain.Submission;
import com.smarthealthdog.backend.domain.SubmissionStatus;

import jakarta.transaction.Transactional;

@Repository  // Optional, but recommended for clarity
public interface SubmissionRepository extends JpaRepository<Submission, UUID> {
    // 개발 전용: 상태별로 가장 오래된 100개의 서브미션 조회
    List<Submission> findFirst100ByStatusOrderBySubmittedAtAsc(SubmissionStatus status);
    // You'd also need the method to fetch the IDs, but for the update, use this:
    
    @Modifying
    @Transactional
    @Query("UPDATE Submission s SET s.status = :newStatus WHERE s.id IN :submissionIds")
    int updateStatusByIds(@Param("newStatus") SubmissionStatus newStatus, @Param("submissionIds") List<UUID> submissionIds);
}
