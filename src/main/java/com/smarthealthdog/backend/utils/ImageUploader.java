package com.smarthealthdog.backend.utils;

import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;
import com.smarthealthdog.backend.domain.Pet;
import com.smarthealthdog.backend.dto.diagnosis.create.SubmissionImageUploadEvent;
import com.smarthealthdog.backend.dto.users.UserProfilePictureUploadEvent;

public interface ImageUploader {

    void uploadProfilePicture(
        UserProfilePictureUploadEvent event
    ) throws IOException;

    String uploadPetImage(
        Pet pet, 
        MultipartFile file
    ) throws IOException;

    void uploadSubmissionImage(
        SubmissionImageUploadEvent event
    ) throws IOException;
}