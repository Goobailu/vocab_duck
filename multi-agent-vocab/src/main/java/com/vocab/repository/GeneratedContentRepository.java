package com.vocab.repository;

import com.vocab.entity.GeneratedContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeneratedContentRepository extends JpaRepository<GeneratedContent, Long> {

    /**
     * Find cached content by word ID and content type.
     * Returns the most recent entry (if multiple exist).
     */
    Optional<GeneratedContent> findTopByWordIdAndContentTypeOrderByCreatedAtDesc(Long wordId, String contentType);

    /**
     * Delete all cached content for a given word.
     */
    void deleteByWordId(Long wordId);

    /**
     * Delete cached content by word ID and content type.
     */
    void deleteByWordIdAndContentType(Long wordId, String contentType);
}
