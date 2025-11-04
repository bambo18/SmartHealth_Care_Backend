package com.smarthealthdog.backend.services;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthealthdog.backend.domain.Shelter;
import com.smarthealthdog.backend.dto.shelter.AdoptionStatus;
import com.smarthealthdog.backend.dto.shelter.ShelterPetItem;
import com.smarthealthdog.backend.dto.shelter.ShelterPetsResponse;
import com.smarthealthdog.backend.dto.shelter.ShelterProfileResponse;
import com.smarthealthdog.backend.exceptions.ResourceNotFoundException;
import com.smarthealthdog.backend.repositories.ShelterRepository;
import com.smarthealthdog.backend.validation.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShelterService {

    private final ShelterRepository shelterRepository;

    /** 보호소 기본 정보 조회 */
    public ShelterProfileResponse getProfile(Long shelterId) {
        Shelter s = shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND));
        return ShelterProfileResponse.from(s);
    }

    /** 보호소 입양 가능 동물 목록 */
    public ShelterPetsResponse listAdoptionPets(Long shelterId, String status, Integer limit, Integer offset) {
        if (limit == null || limit <= 0) limit = 20;
        if (offset == null || offset < 0) offset = 0;

        shelterRepository.findById(shelterId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESOURCE_NOT_FOUND));

        AdoptionStatus st = AdoptionStatus.fromNullable(status);

        // ⚙️ TODO: DB or 외부 API 붙이기 전, 더미로 테스트 가능
        List<ShelterPetItem> items = new ArrayList<>();
        // items.add(new ShelterPetItem(1L, "해피", "dog", "Jindo", "female", "2y", true, "https://example.com/happy.jpg", st.name()));

        return ShelterPetsResponse.of(shelterId, items, limit, offset, st.name());
    }
}