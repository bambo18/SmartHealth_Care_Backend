package com.smarthealthdog.backend.controllers;

import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smarthealthdog.backend.domain.Pet;
import com.smarthealthdog.backend.dto.CreatePetRequest;
import com.smarthealthdog.backend.dto.PetResponse;
import com.smarthealthdog.backend.dto.UpdatePetRequest;
import com.smarthealthdog.backend.services.PetService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

//@RestController 이거는 클래스가 http요청을 받는 컨트롤러임을 명시
@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;

    /** 반려동물 등록 */
    @PostMapping
    public ResponseEntity<PetResponse> create(@RequestBody @Valid CreatePetRequest req) {
        Pet saved = petService.create(req);
        return ResponseEntity.ok(PetResponse.from(saved));
    }

    /** 단건 조회 (명세서: { status, pet } 구조) */
@GetMapping("/{id}")
public ResponseEntity<?> get(@PathVariable Long id) {
    Pet pet = petService.get(id);
    return ResponseEntity.ok(java.util.Map.of(
        "status", 200,
        "pet", PetResponse.from(pet)
    ));
}

    /** 소유자 기준 목록 조회 */
@GetMapping
public ResponseEntity<?> list(@RequestParam Long ownerId) {
    var pets = petService.listByOwner(ownerId)
            .stream()
            .map(PetResponse::from)
            .collect(Collectors.toList());

    // 명세서 구조에 맞게 {"pets": [...] } 형태로 반환
    return ResponseEntity.ok(java.util.Map.of("pets", pets));
}

    /** 전체 수정 */
    @PutMapping("/{id}")
    public ResponseEntity<PetResponse> update(@PathVariable Long id,
                                              @RequestBody @Valid UpdatePetRequest req) {
        Pet updated = petService.update(id, req);
        return ResponseEntity.ok(PetResponse.from(updated));
    }

    /** 삭제 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        petService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /** 부분 수정 (Partial Update) */
    @PatchMapping("/{id}")
    public ResponseEntity<?> partialUpdate(@PathVariable Long id,
                                           @RequestBody java.util.Map<String, Object> updates) {
        Pet updatedPet = petService.partialUpdate(id, updates);
        return ResponseEntity.ok(java.util.Map.of(
                "status", 200,
                "message", "반려동물 정보가 수정되었습니다.",
                "pet", PetResponse.from(updatedPet)
        ));
    }
}
//클라이언트 (json요청) -> petController(요청 수신 & dto변환) ->
//petService(비즈니스 로직, db작업) -> petRepository(jpa퀴러) ->
//db(저장 및 조회) -> PetResponse(json변환 후 응답)