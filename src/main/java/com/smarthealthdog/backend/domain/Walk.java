package com.smarthealthdog.backend.domain;

import java.time.Duration;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "walks")
public class Walk {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long petId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime startTime;

    @Column(columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime endTime;   // 종료 시점 저장

    private Double distance;          // 총 거리 (단위는 정책에 맞춰 사용)

    @Column(columnDefinition = "TEXT")
    private String pathCoordinates;   // 경로(JSON 문자열)

    @Column(name = "duration_seconds")
    private Long durationSeconds;     // end - start (초)

    @Builder
    private Walk(Long petId, Long userId, OffsetDateTime startTime) {
        this.petId = petId;
        this.userId = userId;
        this.startTime = startTime;
        this.pathCoordinates = "[]";
    }

    /** 산책 종료 처리 */
    public void end(OffsetDateTime newStart, OffsetDateTime end, Double distance, String pathJson) {
        // start_time이 요청에 포함되므로 클라이언트와 서버 상태를 맞추려면 선택적으로 갱신/검증
        // 정책: 요청의 start_time이 엔티티와 다르면 엔티티 값을 우선(혹은 동일해야 한다고 검증)
        if (newStart != null) {
            this.startTime = newStart;
        }
        this.endTime = end;
        this.distance = distance;
        this.pathCoordinates = pathJson;

        if (this.startTime != null && this.endTime != null) {
            this.durationSeconds = Duration.between(this.startTime, this.endTime).getSeconds();
        }
    }
}
