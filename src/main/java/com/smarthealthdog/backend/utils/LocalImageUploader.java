package com.smarthealthdog.backend.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.multipart.MultipartFile;

import com.smarthealthdog.backend.domain.Pet;
import com.smarthealthdog.backend.domain.Submission;
import com.smarthealthdog.backend.dto.diagnosis.create.SubmissionImageUploadEvent;
import com.smarthealthdog.backend.dto.users.UserProfilePictureUploadEvent;
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.repositories.PetRepository;
import com.smarthealthdog.backend.repositories.UserRepository;
import com.smarthealthdog.backend.services.SubmissionService;
import com.smarthealthdog.backend.validation.ErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * Development-only implementation to save images to the local static folder.
 */
@Component
@Profile({"dev", "test"})
@RequiredArgsConstructor
public class LocalImageUploader implements ImageUploader {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final SubmissionService submissionService;
    
    // The relative path in src/main/resources/static/
    private final Path uploadDir = Paths.get("uploads/");

    // Helper to save file bytes to a sub-directory
    private String saveFile(byte[] fileBytes, String originalFilename, String subDir) throws IOException {
        if (fileBytes == null || fileBytes.length == 0 || originalFilename == null) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_IMAGE);
        }

        String ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        String newFileName = subDir + UUID.randomUUID() + ext;
        Path targetPath = uploadDir.resolve(newFileName);
        
        // Create parent directories if they don't exist
        Files.createDirectories(targetPath.getParent()); 
        
        Files.write(targetPath, fileBytes);
        
        // Return the URL path relative to the static root for the frontend
        return newFileName.replace("\\", "/"); 
    }

    /**
     * 유저 프로필 사진을 로컬 저장소에 업로드하고 사용자 정보를 업데이트합니다.
     * @param user 업로드할 유저 객체
     * @param fileBytes 업로드할 파일의 바이트 배열
     * @param originalFilename 원본 파일 이름
     * @param contentType 파일의 콘텐츠 타입
     * @throws IOException 파일 저장 중 오류 발생 시
     */
    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void uploadProfilePicture(UserProfilePictureUploadEvent event) throws IOException {
        String key = saveFile(event.fileBytes(), event.originalFilename(), "profiles/");

        event.user().setProfilePic(key);
        userRepository.save(event.user());
    }

    /**
     * 반려동물 프로필 사진을 로컬 저장소에 업로드하고 반려동물 정보를 업데이트합니다.
     * @param pet 업로드할 반려동물 객체
     * @param file 업로드할 파일
     * @return 저장된 파일의 경로
     * @throws IOException 파일 저장 중 오류 발생 시
     */
    @Override
    @Async
    @Transactional
    public String uploadPetImage(Pet pet, MultipartFile file) throws IOException {
        String key = saveFile(file.getBytes(), file.getOriginalFilename(), "pets/");
        
        pet.setProfileImage(key);
        petRepository.save(pet);
        
        return key;
    }

    /**
     * 제출된 진단 이미지 파일을 로컬 저장소에 업로드하고 제출 정보를 업데이트합니다.
     * @param submissionId 제출 ID
     * @param fileBytes 업로드할 파일의 바이트 배열
     * @param originalFilename 원본 파일 이름
     * @param contentType 파일의 콘텐츠 타입
     * @throws IOException 파일 저장 중 오류 발생 시
     */
    // @Override
    // @Async
    // @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // @Transactional(propagation = Propagation.REQUIRES_NEW)
    // public void uploadSubmissionImage(Long submissionId, byte[] fileBytes, String originalFilename, String contentType) throws IOException {
    //     String key;
    //     try {
    //         key = saveFile(fileBytes, originalFilename, "diagnoses/");
    //     } catch (IOException e) {
    //         submissionService.failSubmission(submissionId, "로컬 저장소에 이미지 업로드 실패");
    //         throw e;
    //     }
        
    //     submissionService.getSubmissionById(submissionId).setPhotoUrl(key);
    //     submissionService.saveSubmission(submissionService.getSubmissionById(submissionId));
    // }

    @Override
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void uploadSubmissionImage(SubmissionImageUploadEvent event) throws IOException {
        Submission submission = event.submission();

        String key;
        try {
            key = saveFile(event.fileBytes(), event.originalFilename(), "diagnoses/");
        } catch (IOException e) {
            submissionService.failSubmission(submission, "로컬 저장소에 이미지 업로드 실패");
            return;
        }

        submission.setPhotoUrl(key);
        submissionService.saveSubmission(submission);
    }
}