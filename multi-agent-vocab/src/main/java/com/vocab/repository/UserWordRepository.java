package com.vocab.repository;

import com.vocab.entity.User;
import com.vocab.entity.UserWord;
import com.vocab.entity.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface UserWordRepository extends JpaRepository<UserWord, Long> {

    List<UserWord> findByUserId(Long userId);

    Optional<UserWord> findByUserIdAndWordId(Long userId, Long wordId);

    Optional<UserWord> findByUserAndWord(User user, Word word);

    @Query("SELECT uw.wordId FROM UserWord uw WHERE uw.userId = :userId")
    Set<Long> findLearnedWordIds(Long userId);
    
    @Query("SELECT uw.wordId FROM UserWord uw WHERE uw.userId = :userId AND uw.status = 'mastered'")
    Set<Long> findMasteredWordIds(Long userId);
    
    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, String status);

    @Query("SELECT uw FROM UserWord uw WHERE uw.userId = :userId " +
           "AND (uw.nextReview IS NULL OR uw.nextReview <= :now) AND uw.status IN ('learning','review','new') " +
           "ORDER BY uw.nextReview ASC")
    List<UserWord> findDueWords(Long userId, LocalDateTime now, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT uw FROM UserWord uw WHERE uw.userId = :userId " +
           "AND uw.memoryRetrievability < 0.5 AND uw.status <> 'mastered' " +
           "AND uw.nextReview > :now " +
           "ORDER BY uw.memoryRetrievability ASC")
    List<UserWord> findDecliningWords(Long userId, LocalDateTime now, org.springframework.data.domain.Pageable pageable);

    /**
     * 查找今天和昨天新学习的单词（尚未复习过的），用于在复习区域展示新学内容
     * Find words first learned today or yesterday that have not been reviewed yet
     */
    @Query("SELECT uw FROM UserWord uw WHERE uw.userId = :userId " +
           "AND uw.firstLearned >= :since " +
           "AND uw.reviewCount = 0 " +
           "AND uw.status <> 'mastered' " +
           "ORDER BY uw.firstLearned DESC")
    List<UserWord> findRecentlyLearnedWords(Long userId, LocalDateTime since, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(uw) FROM UserWord uw WHERE uw.userId = :userId " +
           "AND (uw.nextReview IS NULL OR uw.nextReview <= :now) AND uw.status IN :statuses")
    long countByUserIdAndNextReviewBeforeAndStatusIn(Long userId, LocalDateTime now, List<String> statuses);

    /**
     * 统计近期新学但尚未复习的单词数（今天和昨天学的），用于仪表盘"待复习"显示
     * Count recently learned words (today & yesterday) not yet reviewed
     */
    @Query("SELECT COUNT(uw) FROM UserWord uw WHERE uw.userId = :userId " +
           "AND uw.firstLearned >= :since " +
           "AND uw.reviewCount = 0 " +
           "AND uw.status <> 'mastered'")
    long countRecentlyLearnedUnreviewed(Long userId, LocalDateTime since);

    /**
     * 统计今天新学习的单词数量，用于仪表盘"今日学习进度"
     * Count words first learned today
     */
    @Query("SELECT COUNT(uw) FROM UserWord uw WHERE uw.userId = :userId " +
           "AND uw.firstLearned >= :todayStart")
    long countTodayLearnedWords(Long userId, LocalDateTime todayStart);

    @Query("SELECT FUNCTION('DATE', uw.firstLearned) as date, COUNT(uw) as count " +
           "FROM UserWord uw WHERE uw.userId = :userId AND uw.firstLearned >= :since " +
           "GROUP BY FUNCTION('DATE', uw.firstLearned)")
    List<Object[]> countNewWordsByDate(Long userId, LocalDateTime since);

    @Query("SELECT COUNT(uw) FROM UserWord uw WHERE uw.userId = :userId " +
           "AND uw.nextReview >= :start AND uw.nextReview < :end " +
           "AND uw.status IN ('learning','review')")
    long countByDateRange(Long userId, LocalDateTime start, LocalDateTime end);

    @Query("SELECT AVG(uw.masteryLevel) FROM UserWord uw WHERE uw.userId = :userId")
    Double findAvgMastery(Long userId);

    @Query("SELECT uw FROM UserWord uw WHERE uw.userId = :userId ORDER BY uw.lastReviewed DESC NULLS LAST")
    List<UserWord> findRecentWords(Long userId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT uw.status, COUNT(uw) FROM UserWord uw WHERE uw.userId = :userId GROUP BY uw.status")
    List<Object[]> countByStatus(Long userId);

    @Query("SELECT uw.repetitions, COUNT(uw) FROM UserWord uw WHERE uw.userId = :userId " +
           "AND uw.nextReview >= :start AND uw.nextReview <= :end " +
           "AND uw.status IN ('learning','review') " +
           "GROUP BY uw.repetitions")
    List<Object[]> countByRepetitionCycle(Long userId, LocalDateTime start, LocalDateTime end);

    List<UserWord> findByUserIdOrderByLastReviewedDesc(Long userId);

    void deleteByUserId(Long userId);
}
