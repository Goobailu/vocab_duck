package com.vocab.repository;

import com.vocab.entity.Word;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface WordRepository extends JpaRepository<Word, Long> {

    Optional<Word> findByWord(String word);

    /** New words not yet learned by user, sorted by frequency */
    @Query("SELECT w FROM Word w WHERE w.id NOT IN :learnedIds " +
           "AND w.difficulty <= :maxDiff AND w.difficulty >= :minDiff " +
           "ORDER BY w.frequencyRank ASC NULLS LAST, w.difficulty ASC")
    List<Word> findNewWords(Set<Long> learnedIds, int minDiff, int maxDiff, org.springframework.data.domain.Pageable pageable);

    /** New words filtered by library tag, not yet learned by user */
    @Query("SELECT w FROM Word w WHERE w.id NOT IN :learnedIds " +
           "AND w.tags LIKE %:tag% " +
           "ORDER BY w.frequencyRank ASC NULLS LAST, w.id ASC")
    List<Word> findNewWordsByTag(Set<Long> learnedIds, String tag, org.springframework.data.domain.Pageable pageable);

    /** Count total words in a specific library tag */
    @Query("SELECT COUNT(w) FROM Word w WHERE w.tags LIKE %:tag%")
    long countByTag(String tag);
}
