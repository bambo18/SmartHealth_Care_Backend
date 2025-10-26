package com.smarthealthdog.backend.services;

import java.io.IOException;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smarthealthdog.backend.domain.User;
import com.smarthealthdog.backend.exceptions.InvalidRequestDataException;
import com.smarthealthdog.backend.utils.FileUtils;
import com.smarthealthdog.backend.utils.S3Uploader;
import com.smarthealthdog.backend.validation.ErrorCode;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class FileUploadService {
    private final S3Uploader s3Uploader;

    /**
     * S3 버킷에 프로필 사진 업로드
     * @param userId 사용자 ID
     * @param file 업로드할 프로필 사진 파일
     * @return 파일 URL
     * @throws IOException 파일 업로드 중 오류 발생 시
     * @throws InvalidRequestDataException 유효하지 않은 이미지 파일인 경우 발생
     */
    public void uploadProfilePicture(User user, MultipartFile file) throws IOException, InvalidRequestDataException {
        validateImageFile(file);

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes(); // READS THE TEMPORARY FILE NOW (on the main thread)
        } catch (IOException e) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_IMAGE);
        }

        s3Uploader.uploadProfilePicture(user, fileBytes, file.getOriginalFilename(), file.getContentType());
    }

    /**
     * 업로드된 파일이 유효한 이미지 파일인지 검증
     * @param file 업로드된 파일
     * @throws InvalidRequestDataException 이미지 파일이 아닌 경우 발생
     */
    public void validateImageFile(MultipartFile file) throws InvalidRequestDataException {
        if (file == null || file.isEmpty()) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_IMAGE);
        }

        // 파일 크기 검사 (예: 최대 5MB)
        long maxFileSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxFileSize) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_IMAGE);
        }

        // 파일 형식 검사
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !(originalFilename.endsWith(".png") || originalFilename.endsWith(".jpg") || originalFilename.endsWith(".jpeg"))) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_IMAGE);
        }

        // MIME 타입 검사
        try {
            boolean isValidImageFile = FileUtils.isMIMEImage(file.getInputStream());
            if (!isValidImageFile) {
                throw new IllegalArgumentException("유효하지 않은 이미지 파일");
            }
        } catch (IOException | IllegalArgumentException e) {
            throw new InvalidRequestDataException(ErrorCode.INVALID_IMAGE);
        }
    }
}
