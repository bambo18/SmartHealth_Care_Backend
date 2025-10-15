//entity -> response 변환용
//controller가 바로 petresponse를 반환하면 프론트엔드에서 사용하기 쉬워짐

package com.smarthealthdog.backend.dto;

import java.time.LocalDate;

import com.smarthealthdog.backend.domain.Pet;
import com.smarthealthdog.backend.domain.Sex;
import com.smarthealthdog.backend.domain.Species;

//반려동물 정보를 응답으로 보낼 때 사용되는 dto
//서버->클라이언트
public record PetResponse(
        Long id,
        String name,
        Species species,
        String breed,
        Sex sex,
        LocalDate birthDate,
        Boolean neutered,
        Double weightKg,
        Long ownerId
) {
    public static PetResponse from(Pet pet) {
        return new PetResponse(
                pet.getId(),
                pet.getName(),
                pet.getSpecies(),
                pet.getBreed(),
                pet.getSex(),
                pet.getBirthDate(),
                pet.getNeutered(),
                pet.getWeightKg(),
                pet.getOwnerId()
        );
    }
}