//이 파일은 controller -> db 사이의 핵심 로직 처리자 
//예외 처리, 트랜직션, jpa연동을 맡아서 실제 데이터 변경을 책임짐
package com.smarthealthdog.backend.services;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smarthealthdog.backend.domain.Pet;
import com.smarthealthdog.backend.domain.Sex;
import com.smarthealthdog.backend.domain.Species;
import com.smarthealthdog.backend.dto.CreatePetRequest;
import com.smarthealthdog.backend.dto.UpdatePetRequest;
import com.smarthealthdog.backend.repositories.PetRepository;
import com.smarthealthdog.backend.support.NotFoundException;

import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;

    // 반려동물 등록(post)
    public Pet create(CreatePetRequest req) {
        Pet pet = Pet.builder()
                .name(req.name())
                .species(req.species())
                .breed(req.breed())
                .sex(req.sex())
                .birthDate(req.birthDate())
                .neutered(req.neutered())
                .weightKg(req.weightKg())
                .ownerId(req.ownerId())
                .build();
        return petRepository.save(pet);
    }

    // 단건 조회 (get)
    @Transactional(readOnly = true)
    public Pet get(Long id) {
        return petRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Pet not found: " + id));
    }

    // 소유자 기준 목록 조회(get)
    @Transactional(readOnly = true)
    public List<Pet> listByOwner(Long ownerId) {
        return petRepository.findByOwnerId(ownerId);
    }

    // 전체 수정(put)
    public Pet update(Long id, UpdatePetRequest req) {
        Pet p = get(id); // 존재 확인 + 엔티티 로드
        p.update(
                req.name(),
                req.species(),
                req.breed(),
                req.sex(),
                req.birthDate(),
                req.neutered(),
                req.weightKg()
        );
        // JPA 변경감지로 자동 업데이트
        return p;
    }

    // 삭제 (delete) 존재하는지 확인-> 없으면 not뭐시기로 던짐 있으면 삭제 실행
    public void delete(Long id) {
        if (!petRepository.existsById(id)) {
            throw new NotFoundException("Pet not found: " + id);
        }
        petRepository.deleteById(id);
    }

    @Transactional
public Pet partialUpdate(Long id, Map<String, Object> updates) {
    Pet p = get(id); // 존재 확인 + 영속 엔티티 로드

    // 현재 값(기본값)으로 로컬 변수 복사
    String    name     = p.getName();
    Species   species  = p.getSpecies();
    String    breed    = p.getBreed();
    Sex       sex      = p.getSex();
    LocalDate birth    = p.getBirthDate();
    Boolean   neutered = p.getNeutered();
    Double    weightKg = p.getWeightKg();

    // 전달된 필드만 반영 (명세서의 snake_case도 허용)
    if (updates.containsKey("name"))        name = (String) updates.get("name");
    if (updates.containsKey("species"))     species = Species.valueOf(((String) updates.get("species")).toUpperCase());
    if (updates.containsKey("breed"))       breed = (String) updates.get("breed");

    if (updates.containsKey("sex")) {
        sex = Sex.valueOf(((String) updates.get("sex")).toUpperCase());
    } else if (updates.containsKey("gender")) { // 명세서 alias
        sex = Sex.valueOf(((String) updates.get("gender")).toUpperCase());
    }

    if (updates.containsKey("birthDate")) {
        birth = LocalDate.parse((String) updates.get("birthDate"));
    } else if (updates.containsKey("birthdate")) { // 명세서 alias
        birth = LocalDate.parse((String) updates.get("birthdate"));
    }

    if (updates.containsKey("neutered")) {
        neutered = (Boolean) updates.get("neutered");
    } else if (updates.containsKey("is_neutered")) { // 명세서 alias
        neutered = (Boolean) updates.get("is_neutered");
    }

    if (updates.containsKey("weightKg"))    weightKg = Double.valueOf(updates.get("weightKg").toString());

    // 우리 엔티티는 필드별 setter 대신 update(전체) 메서드로 반영
    p.update(name, species, breed, sex, birth, neutered, weightKg);

    // JPA 변경감지로 DB 업데이트
    return p;
}
}
