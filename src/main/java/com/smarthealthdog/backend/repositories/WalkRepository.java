package com.smarthealthdog.backend.repositories;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.smarthealthdog.backend.domain.Walk;

public interface WalkRepository extends JpaRepository<Walk, Long> {

    // 전체 + 최신순/오래된순
    List<Walk> findByPetIdOrderByStartTimeDesc(Long petId);
    List<Walk> findByPetIdOrderByStartTimeAsc(Long petId);

    // 기간 + 최신순/오래된순
    List<Walk> findByPetIdAndStartTimeBetweenOrderByStartTimeDesc(
            Long petId, OffsetDateTime start, OffsetDateTime end);
    List<Walk> findByPetIdAndStartTimeBetweenOrderByStartTimeAsc(
            Long petId, OffsetDateTime start, OffsetDateTime end);

    /** 주간 합계용 네이티브 프로젝션 */
    public interface WeeklyAggRow {
        Long getPetId();
        Long getTotalWalks();
        Double getTotalDistanceKm();
        Long getTotalDurationSec();
    }

    @Query(value = """
        SELECT
            pet_id              AS petId,
            COUNT(*)            AS totalWalks,
            COALESCE(SUM(distance), 0) AS totalDistanceKm,
            COALESCE(SUM(
                CASE WHEN end_time IS NOT NULL
                     THEN EXTRACT(EPOCH FROM (end_time - start_time))
                     ELSE 0 END
            ), 0)               AS totalDurationSec
        FROM walks
        WHERE user_id = :userId
          AND start_time >= :start
          AND start_time <  :end
        GROUP BY pet_id
        """, nativeQuery = true)
    List<WeeklyAggRow> aggregateByUserAndPeriod(
            @Param("userId") Long userId,
            @Param("start") OffsetDateTime start,
            @Param("end")   OffsetDateTime end);

}
