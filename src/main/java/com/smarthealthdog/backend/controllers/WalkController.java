package com.smarthealthdog.backend.controllers;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smarthealthdog.backend.domain.Walk;
import com.smarthealthdog.backend.dto.walk.CreateWalkRequest;
import com.smarthealthdog.backend.dto.walk.EndWalkRequest;
import com.smarthealthdog.backend.dto.walk.WalkResponse;
import com.smarthealthdog.backend.services.WalkService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/pets/{petId}/walks")
@RequiredArgsConstructor
public class WalkController {

    private final WalkService walkService;

    @PostMapping
    public ResponseEntity<?> start(
            @PathVariable Long petId,
            @RequestParam Long userId, // TODO: JWT 적용 시 제거
            @RequestBody @Valid CreateWalkRequest req
    ) {
        Walk saved = walkService.start(petId, userId, req);
        return ResponseEntity.status(201).body(Map.of(
                "status", 201,
                "message", "산책이 시작되었습니다.",
                "walk", WalkResponse.toSnake(saved)
        ));
    }

    /** 산책 종료 */
    @PatchMapping("/{walkId}/end")
    public ResponseEntity<?> end(
            @PathVariable Long petId,
            @PathVariable Long walkId,
            @RequestParam Long userId, // TODO: JWT 적용 시 제거
            @RequestBody @Valid EndWalkRequest req
    ) {
        Walk updated = walkService.end(petId, walkId, userId, req);
        return ResponseEntity.ok(Map.of(
                "status", 200,
                "message", "산책 기록이 업데이트되었습니다.",
                "walk", WalkResponse.toSnake(updated) // durationSeconds 포함반환
        ));
    }

    /** 특정 반려동물 산책 목록 조회 */
    @GetMapping
    public ResponseEntity<?> list(
            @PathVariable Long petId,
            @RequestParam Long userId, // TODO: JWT 적용 시 제거
            @RequestParam(required = false) String start_date, // YYYY-MM-DD
            @RequestParam(required = false) String end_date,   // YYYY-MM-DD
            @RequestParam(required = false, defaultValue = "date_desc") String sort_by,
            @RequestParam(required = false, defaultValue = "20") Integer limit,
            @RequestParam(required = false, defaultValue = "0") Integer offset
    ) {
        // 날짜 파싱 (00:00:00Z ~ 23:59:59Z)
        OffsetDateTime start = null;
        OffsetDateTime end = null;
        try {
            if (start_date != null && !start_date.isBlank())
                start = OffsetDateTime.parse(start_date + "T00:00:00Z");
            if (end_date != null && !end_date.isBlank())
                end = OffsetDateTime.parse(end_date + "T23:59:59Z");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", "유효하지 않은 쿼리 파라미터입니다. 날짜 형식을 확인하세요."
            ));
        }

        // (JWT 붙인 후) 소유권 검증 추가 예정
        List<Walk> all = walkService.listByPet(petId, start, end, sort_by);

        // limit/offset 슬라이싱
        int from = Math.max(0, offset);
        int to = Math.min(all.size(), from + Math.max(0, limit));
        List<Walk> pageSlice = (from < to) ? all.subList(from, to) : List.of();

        var items = pageSlice.stream().map(w -> Map.of(
                "walk_id", w.getId(),
                "start_time", w.getStartTime(),
                "end_time", w.getEndTime(),
                "duration", w.getDurationSeconds(),
                "distance", w.getDistance()
        )).toList();

        return ResponseEntity.ok(Map.of(
                "pet_id", petId,
                "total", all.size(),
                "items", items,
                "page", Map.of(
                        "limit", limit,
                        "offset", offset,
                        "sort_by", sort_by,
                        "start_date", start_date,
                        "end_date", end_date
                )
        ));
    }

    @GetMapping("/api/users/me/walks/summary/weekly-comparison")
    public ResponseEntity<?> weeklyComparison(
            @RequestParam String start_date,
            @RequestParam String end_date,
            @RequestParam Long userId // TODO: JWT 머지 후 제거, 인증정보에서 가져오기
    ) {
        LocalDate start;
        LocalDate end;
        try {
            start = LocalDate.parse(start_date); // YYYY-MM-DD
            end   = LocalDate.parse(end_date);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "status", 400,
                    "message", "start_date/end_date 형식을 확인하세요. (YYYY-MM-DD)"
            ));
        }

        // 정책 타임존 (고정하고 싶으면 ZoneId.of("Asia/Seoul"))
        ZoneId zone = ZoneId.systemDefault();

        var body = walkService.weeklyComparison(userId, start, end, zone);
        return ResponseEntity.ok(body);
    }

}