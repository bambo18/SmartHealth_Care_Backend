package com.smarthealthdog.backend.dto.diagnosis.get;

import com.smarthealthdog.backend.domain.Submission;
import com.smarthealthdog.backend.utils.ImgUtils;

import lombok.RequiredArgsConstructor;

import com.smarthealthdog.backend.domain.Diagnosis;
import com.smarthealthdog.backend.domain.ConditionTranslation;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SubmissionMapper {
    private final ImgUtils imgUtils;

    public SubmissionDetail toSubmissionDetail(
            Submission submission, 
            List<Diagnosis> diagnoses, 
            List<ConditionTranslation> translations
    ) {
        if (submission == null) {
            throw new IllegalArgumentException("서브미션이 null일 수 없습니다.");
        }

        if (diagnoses == null) {
            throw new IllegalArgumentException("진단이 null일 수 없습니다.");
        }

        if (translations == null || translations.isEmpty()) {
            throw new IllegalArgumentException("번역이 null이거나 비어 있을 수 없습니다.");
        }

        return new SubmissionDetail(
            submission.getId(),
            submission.getPet().getId().toString(), // Get pet ID as String
            imgUtils.getImgUrl(submission.getPhotoUrl()),
            submission.getStatus().name(),
            submission.getSubmittedAt(),
            submission.getCompletedAt(),
            submission.getFailureReason(),
            diagnoses.stream()
                .map(diagnosis -> toDiagnosisResult(diagnosis, translations))
                .collect(Collectors.toSet())
        );
    }

    private DiagnosisResult toDiagnosisResult(Diagnosis diagnosis, List<ConditionTranslation> translations) {
        if (diagnosis == null) {
            throw new IllegalArgumentException("진단이 null일 수 없습니다.");
        }

        if (diagnosis.getCondition() == null) {
            throw new IllegalArgumentException("진단의 상태가 null일 수 없습니다.");
        }

        ConditionTranslation translation = translations.stream()
            .filter(t -> t.getCondition().getId().equals(diagnosis.getCondition().getId()))
            .findFirst()
            .orElse(null);

        if (translation == null) {
            throw new IllegalArgumentException("선호하는 언어에 대한 상태 번역이 없습니다.");
        }

        return new DiagnosisResult(
            diagnosis.getProbability(),
            toConditionTranslation(translation)
        );
    }

    private ConditionTranslationResult toConditionTranslation(ConditionTranslation translation) {
        return new ConditionTranslationResult(
            translation.getTranslatedName(),
            translation.getTranslatedDescription()
        );
    }
}