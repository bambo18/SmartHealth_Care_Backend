package com.smarthealthdog.backend.jobs;

import java.util.List;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.smarthealthdog.backend.domain.PetSpecies;
import com.smarthealthdog.backend.domain.Submission;
import com.smarthealthdog.backend.domain.SubmissionStatus;
import com.smarthealthdog.backend.dto.diagnosis.create.RequestDiagnosisData;
import com.smarthealthdog.backend.repositories.SubmissionRepository;
import com.smarthealthdog.backend.utils.DiagnosisTaskRequestClient;
import com.smarthealthdog.backend.utils.ImgUtils;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
@DisallowConcurrentExecution
public class DevPushAIInferenceTasks implements Job {
    private final ImgUtils imgUtils;
    private final SubmissionRepository submissionRepository;
    private final DiagnosisTaskRequestClient diagnosisTaskRequestClient;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) {
        List<Submission> submissions = submissionRepository.findFirst100ByStatusOrderBySubmittedAtAsc(SubmissionStatus.PENDING);
        if (submissions.isEmpty()) {
            System.out.println("No pending submissions found.");
            return;
        }

        List<RequestDiagnosisData> requestDataList = submissions.stream()
            .map(submission -> {
                String imageURL = imgUtils.getImgUrlForAIWorker(submission.getPhotoUrl());
                Long submissionId = submission.getId();
                PetSpecies species = submission.getPet().getSpecies();

                return new RequestDiagnosisData(imageURL, submissionId, species);
            })
            .toList();

        List<Long> submissionIds = submissions.stream()
            .map(Submission::getId)
            .toList();

        diagnosisTaskRequestClient.sendDiagnosisTaskInBatch(requestDataList);

        // Update statuses to PROCESSING
        submissionRepository.updateStatusByIds(SubmissionStatus.PROCESSING, submissionIds);
    }
}
