package com.vocab.controller;

import com.vocab.entity.User;
import com.vocab.entity.UserWord;
import com.vocab.repository.UserRepository;
import com.vocab.repository.UserWordRepository;
import com.vocab.repository.WordRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/learning")
public class WordBankController {

    private static final List<Map<String, Object>> LIBRARIES = new ArrayList<>();

    static {
        LIBRARIES.add(buildLibrary("cet4", "四级核心单词", "CET-4 Core Vocabulary", "大学英语四级考试核心高频词汇", 1, 2, "📘"));
        LIBRARIES.add(buildLibrary("cet6", "六级核心单词", "CET-6 Core Vocabulary", "大学英语六级考试核心高频词汇", 2, 3, "📗"));
        LIBRARIES.add(buildLibrary("kaoyan", "考研单词", "Graduate Entrance Exam", "全国硕士研究生入学考试英语词汇", 3, 5, "📕"));
        LIBRARIES.add(buildLibrary("ielts", "雅思单词", "IELTS Vocabulary", "雅思考试核心高频词汇", 3, 5, "📙"));
        LIBRARIES.add(buildLibrary("toefl", "托福单词", "TOEFL Vocabulary", "托福考试核心高频词汇", 3, 5, "📓"));
    }

    private static Map<String, Object> buildLibrary(String id, String name, String nameEn, String desc,
                                                     int minDiff, int maxDiff, String icon) {
        Map<String, Object> lib = new LinkedHashMap<>();
        lib.put("id", id);
        lib.put("name", name);
        lib.put("nameEn", nameEn);
        lib.put("description", desc);
        lib.put("minDifficulty", minDiff);
        lib.put("maxDifficulty", maxDiff);
        lib.put("icon", icon);
        return lib;
    }

    private final WordRepository wordRepository;
    private final UserRepository userRepository;
    private final UserWordRepository userWordRepository;

    public WordBankController(WordRepository wordRepository, UserRepository userRepository,
                              UserWordRepository userWordRepository) {
        this.wordRepository = wordRepository;
        this.userRepository = userRepository;
        this.userWordRepository = userWordRepository;
    }

    @GetMapping("/libraries")
    public ResponseEntity<?> getLibraries(@AuthenticationPrincipal User user) {
        Set<Long> learnedIds = userWordRepository.findLearnedWordIds(user.getId());
        List<Map<String, Object>> result = new ArrayList<>();

        for (Map<String, Object> lib : LIBRARIES) {
            Map<String, Object> item = new LinkedHashMap<>(lib);
            String tag = (String) lib.get("id");
            long totalWords = wordRepository.countByTag(tag);
            long learnedInLib = countLearnedInLibrary(learnedIds, tag);
            item.put("totalWords", totalWords);
            item.put("learnedWords", learnedInLib);
            result.add(item);
        }

        return ResponseEntity.ok(result);
    }

    @PostMapping("/select-library")
    public ResponseEntity<?> selectLibrary(
            @AuthenticationPrincipal User user,
            @RequestBody Map<String, String> body) {
        String libraryId = body.get("libraryId");
        if (libraryId == null || libraryId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("detail", "libraryId is required"));
        }

        boolean valid = LIBRARIES.stream().anyMatch(l -> libraryId.equals(l.get("id")));
        if (!valid) {
            return ResponseEntity.badRequest().body(Map.of("detail", "Invalid library ID"));
        }

        user.setSelectedLibrary(libraryId);
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "selectedLibrary", libraryId,
                "message", "词库选择成功"
        ));
    }

    @GetMapping("/library-progress")
    public ResponseEntity<?> getLibraryProgress(@AuthenticationPrincipal User user) {
        String library = user.getSelectedLibrary();
        if (library == null || library.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "selectedLibrary", "",
                    "totalWords", 0,
                    "learnedWords", 0,
                    "completed", false
            ));
        }

        long totalWords = wordRepository.countByTag(library);
        Set<Long> learnedIds = userWordRepository.findLearnedWordIds(user.getId());
        long learnedInLib = countLearnedInLibrary(learnedIds, library);

        List<com.vocab.entity.Word> remaining = wordRepository.findNewWordsByTag(
                learnedIds, library, org.springframework.data.domain.PageRequest.of(0, 1));

        boolean completed = remaining.isEmpty() && totalWords > 0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("selectedLibrary", library);
        result.put("totalWords", totalWords);
        result.put("learnedWords", learnedInLib);
        result.put("remainingWords", totalWords - learnedInLib);
        result.put("completed", completed);
        result.put("progress", totalWords > 0 ? Math.round((double) learnedInLib / totalWords * 100.0) / 100.0 : 0.0);

        return ResponseEntity.ok(result);
    }

    private long countLearnedInLibrary(Set<Long> learnedIds, String tag) {
        if (learnedIds.isEmpty()) return 0;
        List<com.vocab.entity.Word> libWords = wordRepository.findNewWordsByTag(
                Collections.emptySet(), tag,
                org.springframework.data.domain.PageRequest.of(0, 10000));
        long count = 0;
        for (com.vocab.entity.Word w : libWords) {
            if (learnedIds.contains(w.getId())) {
                count++;
            }
        }
        return count;
    }
}
