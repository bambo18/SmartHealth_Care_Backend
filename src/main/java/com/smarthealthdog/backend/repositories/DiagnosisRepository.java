package com.smarthealthdog.backend.repositories;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.smarthealthdog.backend.domain.Diagnosis;
import com.smarthealthdog.backend.domain.Submission;

public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {
    List<Diagnosis> findBySubmission(Submission submission);
}
