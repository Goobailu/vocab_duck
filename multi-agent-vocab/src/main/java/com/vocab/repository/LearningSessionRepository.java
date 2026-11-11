package com.vocab.repository;

import com.vocab.entity.LearningSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LearningSessionRepository extends JpaRepository<LearningSession, Long> {

    List<LearningSession> findByUserIdOrderByStartedAtDesc(Long userId);

    @Query("SELECT s FROM LearningSession s WHERE s.userId = :userId AND s.startedAt >= :since ORDER BY s.startedAt ASC")
    List<LearningSession> findByUserIdSince(Long userId, LocalDateTime since);

    List<LearningSession> findByUserIdAndStartedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);
    
    @Query("SELECT s FROM LearningSession s WHERE s.userId = :userId ORDER BY s.startedAt DESC")
    List<LearningSession> findRecentSessions(Long userId, org.springframework.data.domain.Pageable pageable);
}
