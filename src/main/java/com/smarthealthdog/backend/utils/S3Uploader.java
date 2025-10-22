package com.smarthealthdog.backend.utils;

import java.io.IOException;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.smarthealthdog.backend.domain.Pet;
import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.repositories.PetRepository;
import com.smarthealthdog.backend.repositories.UserRepository;
import com.smarthealthdog.backend.validation.ErrorCode;

import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@RequiredArgsConstructor
@Component
public class S3Uploader {
    private final S3Client s3Client;
    private final UserRepository userRepository;
    private final PetRepository petRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    @Value("${cloud.aws.region.static}")
    private String region;

    /**
     * S3 버킷에 파일 업로드
     * @param filePrefix 파일 접두사 (예: "profiles/")
     * @param file 업로드할 파일
     * @return 파일 URL
     * @throws IOException
     */
    @Async
    @Transactional
    public void uploadProfilePicture(User user, MultipartFile file) throws IOException {
        if (user == null) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_IMAGE);
        }

        if (file == null || file.getOriginalFilename() == null) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_IMAGE);
        }

        String ext = file.getOriginalFilename()
                         .substring(file.getOriginalFilename().lastIndexOf("."));
        String key = "profiles/" + UUID.randomUUID() + ext;

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build(),
            RequestBody.fromBytes(file.getBytes())
        );

        user.setProfilePic(key);
        userRepository.save(user);
    }

    @Async
    @Transactional
    public String uploadPetImage(Pet pet, MultipartFile file) throws IOException {
        if (file == null || file.getOriginalFilename() == null) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_IMAGE);
        }

        String ext = file.getOriginalFilename()
                         .substring(file.getOriginalFilename().lastIndexOf("."));
        String key = "pets/" + UUID.randomUUID() + ext;

        s3Client.putObject(
            PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build(),
            RequestBody.fromBytes(file.getBytes())
        );

        pet.setProfileImage(key);
        petRepository.save(pet);

        return key;
    }
}