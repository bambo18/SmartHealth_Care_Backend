package com.smarthealthdog.backend.services;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthealthdog.backend.domain.Pet;
import com.smarthealthdog.backend.domain.Walk;
import com.smarthealthdog.backend.dto.walk.CreateWalkRequest;
import com.smarthealthdog.backend.dto.walk.EndWalkRequest;
import com.smarthealthdog.backend.repositories.WalkRepository;
import com.smarthealthdog.backend.support.NotFoundException;

import lombok.RequiredArgsConstructor;



@Service
@Transactional
@RequiredArgsConstructor
public class WalkService {

    private final WalkRepository walkRepository;
    private final PetService petService;
    private final ObjectMapper objectMapper = new ObjectMapper(); // 간단히 로컬로 사용

    /** 산책 시작 (완료된 코드) */
    public Walk start(Long petId, Long userId, CreateWalkRequest req) {
        Pet pet = petService.get(petId);
        Walk walk = Walk.builder()
                .petId(pet.getId())
                .userId(userId)
                .startTime(req.start_time())
                .build();
        return walkRepository.save(walk);
    }

    /** 산책 종료 */
    public Walk end(Long petId, Long walkId, Long userId, EndWalkRequest req) {
        // 1) 존재 확인
        Walk w = walkRepository.findById(walkId)
                .orElseThrow(() -> new NotFoundException("Walk not found: " + walkId));

        // 2) 요청 경로의 petId와 기록의 petId 일치 확인
        if (!w.getPetId().equals(petId)) {
            throw new NotFoundException("Walk not found for pet: " + petId); // 404로 처리
        }

        // 3) (JWT 붙인 후) 소유권 검증
        // if (!w.getUserId().equals(userId)) throw new UnauthorizedException(...);

        // 4) 유효성 검사
        if (req.end_time().isBefore(req.start_time())) {
            throw new IllegalArgumentException("end_time은 start_time 이후여야 합니다.");
        }
        if (req.distance() < 0) {
            throw new IllegalArgumentException("distance는 0 이상이어야 합니다.");
        }

        // 5) 좌표 JSON 직렬화
        String pathJson = "[]";
        try {
            if (req.path_coordinates() != null) {
                pathJson = objectMapper.writeValueAsString(req.path_coordinates());
            }
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("path_coordinates 형식이 올바르지 않습니다.");
        }

        // 6) 종료 반영
        w.end(req.start_time(), req.end_time(), req.distance(), pathJson);

        // JPA 변경감지로 자동 update
        return w;
    }

    public Walk get(Long walkId) {
        return walkRepository.findById(walkId)
                .orElseThrow(() -> new NotFoundException("Walk not found: " + walkId));
    }

    @Transactional(readOnly = true)
    public List<Walk> listByPet(Long petId, OffsetDateTime start, OffsetDateTime end, String sortBy) {
        String sort = (sortBy == null || sortBy.isBlank()) ? "date_desc" : sortBy;

        if (start == null && end == null) {
            return switch (sort) {
                case "date_asc" -> walkRepository.findByPetIdOrderByStartTimeAsc(petId);
                default         -> walkRepository.findByPetIdOrderByStartTimeDesc(petId);
            };
        }

        OffsetDateTime startSafe = (start != null) ? start : OffsetDateTime.MIN;
        OffsetDateTime endSafe   = (end   != null) ? end   : OffsetDateTime.MAX;

        return switch (sort) {
            case "date_asc" -> walkRepository
                    .findByPetIdAndStartTimeBetweenOrderByStartTimeAsc(petId, startSafe, endSafe);
            default         -> walkRepository
                    .findByPetIdAndStartTimeBetweenOrderByStartTimeDesc(petId, startSafe, endSafe);
        };
    }
    //산책 삭제? 부분 이거는 물어볼 것(참고로 jwt붙이면 userId 파라미터 제거)
    @Transactional
    public void delete(Long walkId, Long userId) {
        var w = walkRepository.findById(walkId)
                .orElseThrow(() -> new NotFoundException("Walk not found: " + walkId));

        // 임시 소유권 검증 (JWT 머지 후 인증정보로 교체)
        if (!w.getUserId().equals(userId)) {
            // 401 성격이지만, 공통 예외체계 없으니 일단 404처럼 감춤 or 별도 UnauthorizedException 만들어도 됨
            throw new NotFoundException("Walk not found: " + walkId);
        }

        walkRepository.deleteById(walkId);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> weeklyComparison(
            Long userId, LocalDate startDate, LocalDate endDate, ZoneId zone) {

        // 날짜 → 기간 경계(포함/제외)로 변환: [start, end+1)
        OffsetDateTime curStart = startDate.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime curEndEx = endDate.plusDays(1).atStartOfDay(zone).toOffsetDateTime();

        // 지난 주: 각 -7일
        OffsetDateTime prevStart = curStart.minusDays(7);
        OffsetDateTime prevEndEx = curEndEx.minusDays(7);

        // 사용자 반려동물 목록(이름 매핑 용)
        List<Pet> pets = petService.listByOwner(userId);
        Map<Long, String> petNameMap = pets.stream()
                .collect(Collectors.toMap(Pet::getId, Pet::getName));

        // 기간별 집계
        Map<Long, WalkRepository.WeeklyAggRow> curAgg = walkRepository
                .aggregateByUserAndPeriod(userId, curStart, curEndEx)
                .stream().collect(Collectors.toMap(WalkRepository.WeeklyAggRow::getPetId, r -> r));

        Map<Long, WalkRepository.WeeklyAggRow> prevAgg = walkRepository
                .aggregateByUserAndPeriod(userId, prevStart, prevEndEx)
                .stream().collect(Collectors.toMap(WalkRepository.WeeklyAggRow::getPetId, r -> r));

        // 응답 구성
        List<Map<String, Object>> petItems = new ArrayList<>();
        for (Pet p : pets) {
            long petId = p.getId();

            var c = curAgg.getOrDefault(petId, emptyAgg(petId));
            var v = prevAgg.getOrDefault(petId, emptyAgg(petId));

            long cWalks = nvl(c.getTotalWalks());
            double cDist = nvl(c.getTotalDistanceKm());
            long cDur   = nvl(c.getTotalDurationSec());

            long vWalks = nvl(v.getTotalWalks());
            double vDist = nvl(v.getTotalDistanceKm());
            long vDur   = nvl(v.getTotalDurationSec());

            Map<String, Object> item = Map.of(
                "pet_id", petId,
                "name",   petNameMap.getOrDefault(petId, "unknown"),
                "current_week_summary", Map.of(
                    "total_walks",        cWalks,
                    "total_distance_km",  round1(cDist),
                    "total_duration_sec", cDur
                ),
                "previous_week_summary", Map.of(
                    "total_walks",        vWalks,
                    "total_distance_km",  round1(vDist),
                    "total_duration_sec", vDur
                ),
                "delta", Map.of(
                    "walks_pct",    pct(vWalks, cWalks),
                    "distance_pct", pct(vDist,  cDist),
                    "duration_pct", pct(vDur,   cDur)
                )
            );
            petItems.add(item);
        }

        return Map.of(
            "user_id", userId,
            "period", Map.of(
                "current",  Map.of("start_date", startDate, "end_date", endDate),
                "previous", Map.of("start_date", startDate.minusDays(7), "end_date", endDate.minusDays(7))
            ),
            "pets", petItems
        );
    }

    // === helpers ===
    private static WalkRepository.WeeklyAggRow emptyAgg(long petId) {
        return new WalkRepository.WeeklyAggRow() {
            public Long getPetId() { return petId; }
            public Long getTotalWalks() { return 0L; }
            public Double getTotalDistanceKm() { return 0.0; }
            public Long getTotalDurationSec() { return 0L; }
        };
    }

    private static long nvl(Long v)    { return v == null ? 0L  : v; }
    private static double nvl(Double v){ return v == null ? 0.0 : v; }

    private static Double pct(double prev, double curr) {
        if (prev == 0) return null; // 정책: 지난주 0이면 null (원하면 100.0로 변경 가능)
        return Math.round(((curr - prev) / prev * 100.0) * 10.0) / 10.0; // 소수1자리 반올림
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}

