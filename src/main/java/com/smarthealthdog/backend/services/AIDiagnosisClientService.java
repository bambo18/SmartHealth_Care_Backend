package com.smarthealthdog.backend.services;

import org.springframework.web.multipart.MultipartFile;

public interface AIDiagnosisClientService {
    /**
     * AI 서비스를 통해 반려동물의 눈 사진을 진단합니다.
     * @param imageFile 진단할 눈 사진 파일
     * @param petId 반려동물 ID
     * @param ownerId 소유자 ID
     */
    void performEyeDiagnosis(
        MultipartFile imageFile, 
        Long petId, 
        Long ownerId
    );
}
