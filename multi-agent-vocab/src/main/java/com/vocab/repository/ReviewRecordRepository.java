package com.vocab.repository;

import com.vocab.entity.ReviewRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ReviewRecordRepository extends JpaRepository<ReviewRecord, Long> {

    List<ReviewRecord> findByUserIdOrderByReviewedAtAsc(Long userId);

    List<ReviewRecord> findByUserIdOrderByReviewedAtDesc(Long userId);

    long countByUserId(Long userId);

    List<ReviewRecord> findByUserIdAndReviewedAtBetweenOrderByReviewedAtDesc(
            Long userId, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndReviewedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT r FROM ReviewRecord r WHERE r.userId = :userId ORDER BY r.reviewedAt DESC")
    List<ReviewRecord> findRecentReviews(Long userId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(r) FROM ReviewRecord r WHERE r.userId = :userId AND r.reviewedAt >= :since")
    long countByUserIdSince(Long userId, LocalDateTime since);

    @Query("SELECT COUNT(r) FROM ReviewRecord r WHERE r.userId = :userId AND r.correct = true AND r.reviewedAt >= :since")
    long countCorrectByUserIdSince(Long userId, LocalDateTime since);

    @Query(value = "SELECT DATE(r.reviewed_at) as date, COUNT(r.id) as count, SUM(CASE WHEN r.correct = 1 THEN 1 ELSE 0 END) as correct " +
           "FROM review_records r WHERE r.user_id = :userId AND r.reviewed_at >= :since " +
           "GROUP BY DATE(r.reviewed_at) ORDER BY date", nativeQuery = true)
    List<Object[]> getDailyReviewStats(Long userId, LocalDateTime since);

    @Query("SELECT AVG(r.quality) FROM ReviewRecord r WHERE r.userId = :userId AND r.reviewedAt >= :since")
    Double getAvgQualitySince(Long userId, LocalDateTime since);

    void deleteByUserId(Long userId);
}
