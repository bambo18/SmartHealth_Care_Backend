package com.smarthealthdog.backend.dto;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smarthealthdog.backend.domain.Walk;
import com.smarthealthdog.backend.dto.walk.WalkResponse;
import com.smarthealthdog.backend.services.WalkService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class WalkQueryController {

    private final WalkService walkService;

    /** 개별 산책 상세 조회 (임시: userId 쿼리파라미터로 소유권 확인) */
    @GetMapping("/api/walks/{walkId}")
    public ResponseEntity<?> getDetail(
            @PathVariable Long walkId,
            @RequestParam Long userId // TODO: JWT 적용 후 제거하고 인증 컨텍스트에서 조회
    ) {
        Walk w = walkService.get(walkId); // 없으면 404(NotFoundException)

        // 임시 소유권 검증 (JWT 머지 후 인증정보로 대체)
        if (!w.getUserId().equals(userId)) {
            return ResponseEntity.status(401).body(Map.of(
                    "status", 401,
                    "message", "유효하지 않거나 만료된 토큰이거나, 접근 권한이 없습니다."
            ));
        }

        return ResponseEntity.ok(Map.of(
                "status", 200,
                "walk", WalkResponse.toDetailSnake(w)
        ));
    }

    @DeleteMapping("/api/walks/{walkId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long walkId,
            @RequestParam Long userId // TODO: JWT 머지 후 제거
    ) {
        walkService.delete(walkId, userId);
        return ResponseEntity.noContent().build(); // 명세 권장 204
    }
}
