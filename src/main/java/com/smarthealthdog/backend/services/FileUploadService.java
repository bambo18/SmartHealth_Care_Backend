package com.smarthealthdog.backend.services;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.smarthealthdog.backend.utils.S3Uploader;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Service
public class FileUploadService {
    private final S3Uploader s3Uploader;

    /**
     * S3 버킷에 프로필 사진 업로드
     * @param fileBytes 파일 바이트 배열
     * @param originalFilename 원본 파일 이름
     * @param contentType 파일 콘텐츠 타입 (예: "image/png")
     * @return 파일 URL
     * @throws Exception
     */
    public String uploadProfilePicture(MultipartFile file) throws Exception {
        // TODO: 파일 크기 및 형식 검증
        // 예: 최대 5MB, PNG/JPEG 형식 등
        // 이미지가 아닌 파일 업로드 시 예외 처리

        return s3Uploader.uploadFile("profiles/", file);
    }
}
