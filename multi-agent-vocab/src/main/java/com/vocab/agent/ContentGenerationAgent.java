package com.vocab.agent;

import com.fasterxml.jackson.core.type.TypeReference;
import com.vocab.entity.GeneratedContent;
import com.vocab.entity.User;
import com.vocab.entity.Word;
import com.vocab.repository.GeneratedContentRepository;
import com.vocab.repository.WordRepository;
import com.vocab.service.ImageGenerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Content generation agent that produces personalized learning cards,
 * image prompts, quizzes, and generates images for vocabulary words.
 * 
 * Enhanced features:
 * - AI image generation via external API
 * - Dynamic difficulty adjustment based on user's memory diagnosis
 * - Personalized content based on memory style (visual, auditory, semantic)
 */
@Component
public class ContentGenerationAgent extends BaseAgent {

    private static final Logger log = LoggerFactory.getLogger(ContentGenerationAgent.class);

    @Autowired
    private WordRepository wordRepository;

    @Autowired
    private GeneratedContentRepository generatedContentRepository;

    @Autowired
    private ImageGenerationService imageGenerationService;

    private static final String SYSTEM_PROMPT =
            "You are an expert English vocabulary teacher specializing in helping Chinese students " +
            "prepare for CET-4, CET-6, 考研 (postgraduate entrance exam), IELTS, and TOEFL. " +
            "You have deep knowledge of English etymology, mnemonics, collocations, and exam patterns. " +
            "Your explanations are clear, engaging, and tailored to the student's level. " +
            "Always respond in valid JSON format as specified. " +
            "Use Chinese for translations and explanations where appropriate, " +
            "but keep example sentences and linguistic content in English.";

    // ------------------------------------------------------------------ getName
    @Override
    public String getName() {
        return "content";
    }

    // -------------------------------------------------------- generateCard
    /**
     * Generates a complete learning card for the given word, personalised to the user's
     * target exam and difficulty level.  Falls back to deterministic content when the
     * LLM is unavailable or returns an unparseable response.
     */
    public Map<String, Object> generateCard(User user, Word word) {
        if (word == null) {
            log.warn("generateCard called with null word");
            return Collections.emptyMap();
        }

        // --- Cache check: return cached card if available ---
        try {
            java.util.Optional<GeneratedContent> cached =
                    generatedContentRepository.findTopByWordIdAndContentTypeOrderByCreatedAtDesc(
                            word.getId(), "card");
            if (cached.isPresent() && cached.get().getContentJson() != null
                    && !cached.get().getContentJson().isBlank()) {
                Map<String, Object> cachedCard = objectMapper.readValue(
                        cached.get().getContentJson(),
                        new TypeReference<Map<String, Object>>() {});
                log.info("Cache HIT for word '{}' card", word.getWord());
                return cachedCard;
            }
        } catch (Exception e) {
            log.debug("Cache read failed for word '{}': {}", word.getWord(), e.getMessage());
        }

        String levelDesc = resolveLevelDescription(user);
        String examContext = resolveExamContext(user);

        String userPrompt = String.format(
                "Generate a comprehensive vocabulary learning card as JSON for the word \"%s\".\n\n" +
                "Word details from database:\n" +
                "- Phonetic: %s\n" +
                "- Part of speech: %s\n" +
                "- English definition: %s\n" +
                "- Chinese definition: %s\n" +
                "- Etymology: %s\n" +
                "- Example sentences: %s\n" +
                "- Synonyms: %s\n\n" +
                "Student profile:\n" +
                "- Target exam: %s\n" +
                "- Difficulty level: %d (%s)\n\n" +
                "Return EXACTLY this JSON structure (no extra keys):\n" +
                "{\n" +
                "  \"word\": \"%s\",\n" +
                "  \"phonetic\": \"IPA phonetic transcription\",\n" +
                "  \"definitions\": [\n" +
                "    {\"pos\": \"part of speech\", \"en\": \"English definition\", \"cn\": \"中文释义\"}\n" +
                "  ],\n" +
                "  \"mnemonic\": {\n" +
                "    \"method\": \"词根词缀 or 联想 or 谐音 or 故事\",\n" +
                "    \"content\": \"Detailed mnemonic explanation in Chinese\",\n" +
                "    \"visual_hint\": \"A vivid visual description to aid memory\"\n" +
                "  },\n" +
                "  \"etymology\": {\n" +
                "    \"roots\": [{\"part\": \"morpheme\", \"meaning\": \"its meaning\"}],\n" +
                "    \"breakdown\": \"Full etymological breakdown in Chinese\"\n" +
                "  },\n" +
                "  \"examples\": [\n" +
                "    {\"en\": \"English sentence\", \"cn\": \"中文翻译\", \"context\": \"usage context label\"}\n" +
                "  ],\n" +
                "  \"collocations\": [\"common collocation 1\", \"common collocation 2\"],\n" +
                "  \"synonyms\": [\n" +
                "    {\"word\": \"synonym\", \"diff\": \"brief difference in usage\"}\n" +
                "  ],\n" +
                "  \"exam_tips\": \"Exam-specific tips in Chinese for %s\",\n" +
                "  \"mini_story\": {\n" +
                "    \"title\": \"A short catchy title\",\n" +
                "    \"content\": \"A 2-3 sentence micro-story using the word, in English with key phrases highlighted\"\n" +
                "  },\n" +
                "  \"image_prompt\": \"A detailed English prompt for AI image generation that visually represents the word in a memorable scene\"\n" +
                "}\n\n" +
                "Requirements:\n" +
                "- Provide at least 2 definitions if the word has multiple parts of speech.\n" +
                "- Provide at least 3 example sentences at appropriate difficulty.\n" +
                "- Provide at least 3 collocations.\n" +
                "- Provide at least 2 synonyms with clear differentiation.\n" +
                "- The mnemonic should be creative and easy to remember.\n" +
                "- The mini_story should be engaging and use the word naturally.\n" +
                "- The image_prompt should describe a vivid, concrete scene (no text in image).\n" +
                "- Tailor exam_tips specifically to %s format and common question types.\n",
                word.getWord(),
                safe(word.getPhonetic()),
                safe(word.getPartOfSpeech()),
                safe(word.getDefinitionEn()),
                safe(word.getDefinitionCn()),
                safe(word.getEtymology()),
                safe(word.getExampleSentences()),
                safe(word.getSynonyms()),
                examContext,
                user != null ? user.getDifficultyLevel() : 3,
                levelDesc,
                word.getWord(),
                examContext,
                examContext
        );

        Map<String, Object> result = callLlmJson(SYSTEM_PROMPT, userPrompt, 0.7);

        if (result == null || result.isEmpty() || result.containsKey("raw")) {
            log.warn("LLM returned unusable response for word '{}', using fallback", word.getWord());
            return _fallbackContent(word);
        }

        // Ensure the word field is always present
        result.putIfAbsent("word", word.getWord());

        // --- Write to cache ---
        try {
            String json = objectMapper.writeValueAsString(result);
            GeneratedContent gc = GeneratedContent.builder()
                    .wordId(word.getId())
                    .contentType("card")
                    .contentText("Learning card for " + word.getWord())
                    .contentJson(json)
                    .difficulty(user != null ? user.getDifficultyLevel() : 3)
                    .build();
            generatedContentRepository.save(gc);
            log.info("Cached card for word '{}'", word.getWord());
        } catch (Exception e) {
            log.debug("Failed to cache card for word '{}': {}", word.getWord(), e.getMessage());
        }

        return result;
    }

    // ---------------------------------------------------- generateImagePrompt
    /**
     * Generates a detailed image prompt for visual memory reinforcement.
     * Returns a plain English prompt suitable for an AI image generation model.
     */
    public String generateImagePrompt(Word word, User user) {
        if (word == null) {
            return "";
        }

        // --- Cache check ---
        try {
            java.util.Optional<GeneratedContent> cached =
                    generatedContentRepository.findTopByWordIdAndContentTypeOrderByCreatedAtDesc(
                            word.getId(), "image_prompt");
            if (cached.isPresent() && cached.get().getContentText() != null
                    && !cached.get().getContentText().isBlank()) {
                log.info("Cache HIT for word '{}' image_prompt", word.getWord());
                return cached.get().getContentText();
            }
        } catch (Exception e) {
            log.debug("Cache read failed for word '{}' image_prompt: {}", word.getWord(), e.getMessage());
        }

        String fallbackPrompt = buildFallbackImagePrompt(word);

        if (!llmConfig.isLlmConfigured()) {
            log.info("LLM not configured, using fallback prompt for word '{}'", word.getWord());
            return fallbackPrompt;
        }

        String levelHint = user != null
                ? "The scene should be appropriate for a student preparing for " + resolveExamContext(user) + "."
                : "";

        String userPrompt = String.format(
                "Create a detailed image generation prompt for the vocabulary word \"%s\".\n\n" +
                "Word info:\n" +
                "- Definition (EN): %s\n" +
                "- Definition (CN): %s\n" +
                "- Part of speech: %s\n" +
                "- Etymology: %s\n\n" +
                "%s\n\n" +
                "Requirements for the image prompt:\n" +
                "- Write entirely in English.\n" +
                "- Describe a single vivid, memorable scene that visually conveys the word's meaning.\n" +
                "- Use a concrete, real-world setting (no abstract concepts).\n" +
                "- Include specific visual details: colours, lighting, objects, people, actions.\n" +
                "- The scene should be distinctive so the student instantly recalls the word when seeing the image.\n" +
                "- Do NOT include any text, labels, or words in the image description.\n" +
                "- Keep it under 120 words.\n\n" +
                "Return ONLY the image prompt text, nothing else.\n",
                word.getWord(),
                safe(word.getDefinitionEn()),
                safe(word.getDefinitionCn()),
                safe(word.getPartOfSpeech()),
                safe(word.getEtymology()),
                levelHint
        );

        String result;
        try {
            result = callLlm(SYSTEM_PROMPT, userPrompt, 0.8);
        } catch (Exception e) {
            log.warn("LLM call failed for word '{}': {}", word.getWord(), e.getMessage());
            return fallbackPrompt;
        }

        if (result == null || result.isBlank()) {
            log.warn("LLM returned empty image prompt for '{}', using fallback", word.getWord());
            return fallbackPrompt;
        }

        String trimmed = result.trim();

        // --- Write to cache ---
        try {
            GeneratedContent gc = GeneratedContent.builder()
                    .wordId(word.getId())
                    .contentType("image_prompt")
                    .contentText(trimmed)
                    .difficulty(user != null ? user.getDifficultyLevel() : 3)
                    .build();
            generatedContentRepository.save(gc);
            log.info("Cached image_prompt for word '{}'", word.getWord());
        } catch (Exception e) {
            log.debug("Failed to cache image_prompt for word '{}': {}", word.getWord(), e.getMessage());
        }

        return trimmed;
    }

    // ---------------------------------------------------------- generateQuiz
    /**
     * Generates a 5-question choice quiz (English word + 4 Chinese options).
     */
    public Map<String, Object> generateQuiz(User user, List<Word> words) {
        if (words == null || words.isEmpty()) {
            log.warn("generateQuiz called with empty word list");
            return Collections.emptyMap();
        }

        log.info("generateQuiz: words count = {}", words.size());

        Map<String, Object> quiz = new LinkedHashMap<>();
        quiz.put("title", "单词释义选择题");

        List<Map<String, Object>> questions = new ArrayList<>();
        Random random = new Random();

        int count = Math.min(5, words.size());
        for (int i = 0; i < count; i++) {
            Word word = words.get(i);
            if (word == null) continue;

            log.info("generateQuiz: processing word {}: {}", i + 1, word.getWord());

            Map<String, Object> question = new LinkedHashMap<>();
            question.put("id", i + 1);
            question.put("type", "choice");
            question.put("difficulty", 3);
            question.put("question", word.getWord());
            question.put("phonetic", word.getPhonetic());

            String correctAnswer = word.getDefinitionCn();
            if (correctAnswer == null || correctAnswer.isBlank()) {
                correctAnswer = word.getDefinitionEn();
            }

            log.info("generateQuiz: word {} correctAnswer = {}", word.getWord(), correctAnswer);

            List<String> options = new ArrayList<>();
            options.add(correctAnswer);

            List<String> distractors = generateDistractors(words, word);
            for (String distractor : distractors) {
                if (options.size() >= 4) break;
                if (!options.contains(distractor)) {
                    options.add(distractor);
                }
            }

            while (options.size() < 4) {
                String filler = generateFillerOption();
                if (!options.contains(filler)) {
                    options.add(filler);
                }
            }

            Collections.shuffle(options, random);

            int correctIndex = options.indexOf(correctAnswer);

            log.info("generateQuiz: word {} options = {}, correctIndex = {}", word.getWord(), options, correctIndex);

            question.put("options", options);
            question.put("answer", correctIndex);
            question.put("explanation", "正确答案：" + correctAnswer + "\n" + word.getWord() + " 的中文释义是：" + correctAnswer);

            questions.add(question);
        }

        log.info("generateQuiz: generated {} questions", questions.size());

        quiz.put("questions", questions);
        return quiz;
    }

    private List<String> generateDistractors(List<Word> words, Word targetWord) {
        List<String> distractors = new ArrayList<>();
        Random random = new Random();

        String targetDef = targetWord.getDefinitionCn();
        if (targetDef == null) {
            targetDef = targetWord.getDefinitionEn();
        }

        List<Word> otherWords = words.stream()
                .filter(w -> !w.getId().equals(targetWord.getId()))
                .collect(Collectors.toList());

        Collections.shuffle(otherWords, random);

        for (Word w : otherWords) {
            if (distractors.size() >= 3) break;
            String def = w.getDefinitionCn();
            if (def != null && !def.isBlank()) {
                if (!def.equals(targetDef)) {
                    distractors.add(def);
                }
            }
        }

        return distractors;
    }

    private String generateFillerOption() {
        String[] fillers = {
                "形容词", "名词", "动词", "副词",
                "重要的", "困难的", "简单的", "复杂的",
                "学习", "工作", "生活", "时间",
                "美丽的", "快乐的", "悲伤的", "有趣的"
        };
        return fillers[new Random().nextInt(fillers.length)];
    }

    // ---------------------------------------------------- generateWordCard
    /**
     * Generates a learning card for a word identified by its text string.
     * Looks up the Word entity and delegates to generateCard.
     */
    public Map<String, Object> generateWordCard(User user, String wordText) {
        if (wordText == null || wordText.isBlank()) {
            log.warn("generateWordCard called with blank wordText");
            Map<String, Object> errorCard = new LinkedHashMap<>();
            errorCard.put("type", "error");
            errorCard.put("message", "请提供要解释的单词");
            return errorCard;
        }
        java.util.Optional<Word> wordOpt = wordRepository.findByWord(wordText.trim().toLowerCase());
        if (wordOpt.isEmpty()) {
            log.warn("Word not found in database: {}", wordText);
            Map<String, Object> errorCard = new LinkedHashMap<>();
            errorCard.put("type", "error");
            errorCard.put("message", "抱歉，词库中没有找到单词 \"" + wordText + "\"。请尝试其他单词，或者先学习一些新词。");
            return errorCard;
        }
        return generateCard(user, wordOpt.get());
    }

    // ---------------------------------------------------- generateReviewCard
    /**
     * Generates a review card for a word identified by its text string.
     * Uses cached content if available, otherwise generates deterministic fallback content.
     */
    public Map<String, Object> generateReviewCard(User user, String wordText) {
        if (wordText == null || wordText.isBlank()) {
            log.warn("generateReviewCard called with blank wordText");
            Map<String, Object> errorCard = new LinkedHashMap<>();
            errorCard.put("type", "error");
            errorCard.put("message", "请提供要复习的单词");
            return errorCard;
        }
        
        java.util.Optional<Word> wordOpt = wordRepository.findByWord(wordText.trim().toLowerCase());
        if (wordOpt.isEmpty()) {
            log.warn("Word not found in database for review: {}", wordText);
            Map<String, Object> errorCard = new LinkedHashMap<>();
            errorCard.put("type", "error");
            errorCard.put("message", "抱歉，词库中没有找到单词 \"" + wordText + "\"");
            return errorCard;
        }
        
        Word word = wordOpt.get();
        
        try {
            java.util.Optional<GeneratedContent> cached =
                    generatedContentRepository.findTopByWordIdAndContentTypeOrderByCreatedAtDesc(
                            word.getId(), "card");
            if (cached.isPresent() && cached.get().getContentJson() != null
                    && !cached.get().getContentJson().isBlank()) {
                Map<String, Object> cachedCard = objectMapper.readValue(
                        cached.get().getContentJson(),
                        new TypeReference<Map<String, Object>>() {});
                log.debug("Cache HIT for review word '{}'", word.getWord());
                return cachedCard;
            }
        } catch (Exception e) {
            log.debug("Cache read failed for word '{}': {}", word.getWord(), e.getMessage());
        }
        
        return _fallbackContent(word);
    }

    // ========================================================= ENHANCED FEATURES

    /**
     * Generates an AI image for the word using the ImageGenerationService.
     * Combines prompt generation with image API calls.
     */
    public Map<String, Object> generateWordImage(User user, Word word, String imageSize) {
        Map<String, Object> result = new LinkedHashMap<>();

        if (word == null) {
            result.put("status", "error");
            result.put("message", "Word cannot be null");
            return result;
        }

        try {
            java.util.Optional<GeneratedContent> cached =
                    generatedContentRepository.findTopByWordIdAndContentTypeOrderByCreatedAtDesc(
                            word.getId(), "image");
            if (cached.isPresent() && cached.get().getContentJson() != null
                    && !cached.get().getContentJson().isBlank()) {
                Map<String, Object> cachedImage = objectMapper.readValue(
                        cached.get().getContentJson(),
                        new TypeReference<Map<String, Object>>() {});
                if ("success".equals(cachedImage.get("status"))) {
                    log.info("Cache HIT for word '{}' image", word.getWord());
                    return cachedImage;
                }
            }
        } catch (Exception e) {
            log.debug("Cache read failed for word '{}' image: {}", word.getWord(), e.getMessage());
        }

        if (!imageGenerationService.isAvailable()) {
            result.put("status", "error");
            result.put("message", "图像生成API未配置，请在系统设置中配置API密钥");
            return result;
        }

        String prompt = generateImagePrompt(word, user);
        result.put("prompt", prompt);

        Map<String, Object> imageResult = imageGenerationService.generateImage(prompt, imageSize, "standard");
        result.putAll(imageResult);

        if ("success".equals(imageResult.get("status"))) {
            try {
                String json = objectMapper.writeValueAsString(result);
                GeneratedContent gc = GeneratedContent.builder()
                        .wordId(word.getId())
                        .contentType("image")
                        .contentText("Generated image for " + word.getWord())
                        .contentJson(json)
                        .difficulty(user != null ? user.getDifficultyLevel() : 3)
                        .build();
                generatedContentRepository.save(gc);
                log.info("Cached generated image for word '{}'", word.getWord());
            } catch (Exception e) {
                log.debug("Failed to cache image for word '{}': {}", word.getWord(), e.getMessage());
            }
        }

        return result;
    }

    /**
     * Generates content with dynamic difficulty adjustment based on user's memory diagnosis.
     * Adjusts the level of examples, explanations, and complexity based on:
     * - User's memory strength
     * - User's learning efficiency
     * - User's memory style (visual, auditory, semantic)
     */
    public Map<String, Object> generatePersonalizedCard(User user, Word word, 
            Map<String, Object> diagnosisData) {
        Map<String, Object> card = generateCard(user, word);

        if (diagnosisData == null || diagnosisData.isEmpty()) {
            return card;
        }

        Map<String, Object> memoryStyleMap = (Map<String, Object>) diagnosisData.get("memoryStyle");
        String memoryStyle = memoryStyleMap != null ? (String) memoryStyleMap.get("style") : null;
        Double memoryStrength = diagnosisData.get("memoryStrength") instanceof Double ? (Double) diagnosisData.get("memoryStrength") : null;
        

        if (memoryStyle != null) {
            Map<String, Object> enhancement = new LinkedHashMap<>();
            enhancement.put("memoryStyle", memoryStyle);
            
            switch (memoryStyle) {
                case "VISUAL":
                    enhancement.put("styleNote", "视觉型学习者");
                    enhancement.put("recommendedStrategies", 
                            List.of("使用图像联想记忆", "制作单词卡片", "多看例句配图"));
                    card.put("memoryStyleEnhancement", enhancement);
                    break;
                case "AUDITORY":
                    enhancement.put("styleNote", "听觉型学习者");
                    enhancement.put("recommendedStrategies", 
                            List.of("多听单词发音", "跟读例句", "录音自我测试"));
                    card.put("memoryStyleEnhancement", enhancement);
                    break;
                case "SEMANTIC":
                    enhancement.put("styleNote", "语义型学习者");
                    enhancement.put("recommendedStrategies", 
                            List.of("深入理解词义", "使用例句语境", "构建知识网络"));
                    card.put("memoryStyleEnhancement", enhancement);
                    break;
                default:
                    enhancement.put("styleNote", "混合型学习者");
                    enhancement.put("recommendedStrategies", 
                            List.of("尝试多种学习方法", "结合视觉和语境记忆"));
                    card.put("memoryStyleEnhancement", enhancement);
            }
        }

        if (memoryStrength != null && memoryStrength < 0.3) {
            card.put("difficultyAdjustment", "降低难度");
            card.put("adjustmentReason", "记忆强度较低，建议减少每日学习量，增加复习频率");
        } else if (memoryStrength != null && memoryStrength > 1.5) {
            card.put("difficultyAdjustment", "增加难度");
            card.put("adjustmentReason", "记忆强度优秀，建议尝试更高难度的单词");
        }

        return card;
    }

    /**
     * Generates a comprehensive learning package for a word including:
     * - Learning card
     * - Image prompt
     * - Generated image (if available)
     * - Quiz question
     */
    public Map<String, Object> generateLearningPackage(User user, Word word) {
        Map<String, Object> packageResult = new LinkedHashMap<>();
        packageResult.put("word", word.getWord());
        packageResult.put("wordId", word.getId());

        packageResult.put("card", generateCard(user, word));
        packageResult.put("imagePrompt", generateImagePrompt(word, user));
        
        if (imageGenerationService.isAvailable()) {
            packageResult.put("image", generateWordImage(user, word, "1024x1024"));
        } else {
            packageResult.put("image", Map.of("status", "unavailable", "message", "图像生成API未配置"));
        }

        List<Word> singleWordList = Collections.singletonList(word);
        Map<String, Object> quiz = generateQuiz(user, singleWordList);
        packageResult.put("quiz", quiz);

        log.info("Generated learning package for word '{}'", word.getWord());
        return packageResult;
    }

    // ========================================================= PRIVATE HELPERS

    /**
     * Returns a human-readable level description for the user's difficulty level.
     */
    private String resolveLevelDescription(User user) {
        int level = user != null && user.getDifficultyLevel() != null ? user.getDifficultyLevel() : 3;
        switch (level) {
            case 1: return "初中水平 (Junior high school)";
            case 2: return "高中水平 (Senior high school)";
            case 3: return "四六级水平 (CET-4/CET-6)";
            case 4: return "考研雅思水平 (Postgraduate entrance exam / IELTS)";
            case 5: return "托福GRE水平 (TOEFL / GRE)";
            default: return "四六级水平 (CET-4/CET-6)";
        }
    }

    /**
     * Returns a human-readable exam context string.
     */
    private String resolveExamContext(User user) {
        if (user == null || user.getTargetExam() == null) {
            return "CET-6";
        }
        String exam = user.getTargetExam().toLowerCase();
        switch (exam) {
            case "cet4": return "CET-4 (大学英语四级)";
            case "cet6": return "CET-6 (大学英语六级)";
            case "kaoyan": return "考研 (Postgraduate entrance exam)";
            case "ielts": return "IELTS (雅思)";
            case "toefl": return "TOEFL (托福)";
            case "gre": return "GRE";
            default: return user.getTargetExam();
        }
    }

    /**
     * Null-safe string helper — returns empty string for null input.
     */
    private String safe(String value) {
        return value != null ? value : "";
    }

    // -------------------------------------------------- Fallback: card content
    /**
     * Deterministic fallback learning card when LLM is unavailable.
     */
    private Map<String, Object> _fallbackContent(Word word) {
        Map<String, Object> card = new LinkedHashMap<>();
        card.put("word", word.getWord());
        card.put("phonetic", word.getPhonetic() != null ? word.getPhonetic() : "");

        // Definitions - support multiple parts of speech
        List<Map<String, Object>> definitions = new ArrayList<>();
        if (word.getPartOfSpeech() != null && !word.getPartOfSpeech().isBlank()) {
            String[] posParts = word.getPartOfSpeech().split("[,;，；]");
            String[] cnParts = word.getDefinitionCn() != null ? word.getDefinitionCn().split("[,;，；]") : new String[]{};
            String[] enParts = word.getDefinitionEn() != null ? word.getDefinitionEn().split("[,;，；]") : new String[]{};
            
            for (int i = 0; i < posParts.length; i++) {
                Map<String, Object> def = new LinkedHashMap<>();
                def.put("pos", posParts[i].trim());
                def.put("en", i < enParts.length ? enParts[i].trim() : (word.getDefinitionEn() != null ? word.getDefinitionEn() : "No definition available"));
                def.put("cn", i < cnParts.length ? cnParts[i].trim() : (word.getDefinitionCn() != null ? word.getDefinitionCn() : "暂无释义"));
                definitions.add(def);
            }
        } else {
            Map<String, Object> def = new LinkedHashMap<>();
            def.put("pos", "unknown");
            def.put("en", word.getDefinitionEn() != null ? word.getDefinitionEn() : "No definition available");
            def.put("cn", word.getDefinitionCn() != null ? word.getDefinitionCn() : "暂无释义");
            definitions.add(def);
        }
        card.put("definitions", definitions);

        // Mnemonic - generate simple mnemonic based on word structure
        Map<String, Object> mnemonic = new LinkedHashMap<>();
        String wordText = word.getWord();
        String mnemonicContent = generateSimpleMnemonic(wordText, word.getDefinitionCn());
        mnemonic.put("method", "联想记忆");
        mnemonic.put("content", mnemonicContent);
        mnemonic.put("visual_hint", "Imagine a scene related to '" + wordText + "': " + safe(word.getDefinitionCn()));
        card.put("mnemonic", mnemonic);

        // Etymology
        Map<String, Object> etymology = new LinkedHashMap<>();
        if (word.getEtymology() != null && !word.getEtymology().isBlank()) {
            etymology.put("roots", List.of(Map.of("part", word.getWord(), "meaning", word.getEtymology())));
            etymology.put("breakdown", word.getEtymology());
        } else {
            etymology.put("roots", extractRoots(wordText));
            etymology.put("breakdown", "暂无详细词源信息。可以通过词根词缀辅助记忆。");
        }
        card.put("etymology", etymology);

        // Examples — parse from JSON string if available, or generate simple ones
        List<Map<String, Object>> examples = parseExamples(word.getExampleSentences());
        if (examples.isEmpty() && word.getDefinitionEn() != null) {
            examples.add(Map.of("en", "This is an example of using the word: " + wordText + ".", "cn", "这是使用单词 " + wordText + " 的例子。", "context", "general"));
            examples.add(Map.of("en", "She used " + wordText + " in her sentence.", "cn", "她在句子中使用了 " + wordText + "。", "context", "general"));
        }
        card.put("examples", examples);

        // Collocations — generate simple collocations
        List<String> collocations = generateCollocations(wordText, word.getPartOfSpeech());
        card.put("collocations", collocations);

        // Synonyms
        card.put("synonyms", parseSynonyms(word.getSynonyms()));

        // Exam tips
        card.put("exam_tips", "此单词常见于考试中，请重点关注其含义和用法。建议结合例句理解记忆。");

        // Mini story
        Map<String, Object> miniStory = new LinkedHashMap<>();
        miniStory.put("title", "Using " + wordText);
        miniStory.put("content", "Learn to use '" + wordText + "' in context. " + safe(word.getDefinitionEn()) + ". Practice makes perfect!");
        card.put("mini_story", miniStory);

        // Image prompt
        card.put("image_prompt", buildFallbackImagePrompt(word));

        return card;
    }

    private String generateSimpleMnemonic(String word, String definition) {
        if (word == null || word.length() < 3) {
            return "请多读多写，通过重复加深记忆。";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("单词 \"").append(word).append("\" 可以这样记：");
        
        // Try to find common prefixes/suffixes
        Map<String, String> prefixes = new LinkedHashMap<>();
        prefixes.put("un-", "不，否定");
        prefixes.put("re-", "再次，重新");
        prefixes.put("pre-", "前，预先");
        prefixes.put("dis-", "不，分离");
        prefixes.put("in-", "不，向内");
        prefixes.put("im-", "不，向内");
        prefixes.put("ex-", "出，外");
        prefixes.put("sub-", "下，次");
        prefixes.put("super-", "超，上");
        prefixes.put("inter-", "相互，之间");
        prefixes.put("trans-", "跨越，转移");
        prefixes.put("con-", "共同，一起");
        prefixes.put("com-", "共同，一起");
        prefixes.put("de-", "向下，去除");
        prefixes.put("pro-", "向前，支持");
        prefixes.put("anti-", "反对");
        prefixes.put("auto-", "自己");
        prefixes.put("bi-", "二，双");
        prefixes.put("micro-", "微");
        prefixes.put("macro-", "宏");
        prefixes.put("mis-", "错误");
        prefixes.put("over-", "过度");
        prefixes.put("under-", "不足");
        prefixes.put("out-", "超过");
        
        Map<String, String> suffixes = new LinkedHashMap<>();
        suffixes.put("-tion", "名词后缀，表示行为或状态");
        suffixes.put("-sion", "名词后缀，表示行为或状态");
        suffixes.put("-ment", "名词后缀，表示行为或结果");
        suffixes.put("-ness", "名词后缀，表示性质或状态");
        suffixes.put("-able", "形容词后缀，表示能够");
        suffixes.put("-ible", "形容词后缀，表示能够");
        suffixes.put("-ful", "形容词后缀，表示充满");
        suffixes.put("-less", "形容词后缀，表示没有");
        suffixes.put("-ly", "副词后缀");
        suffixes.put("-er", "名词后缀，表示人或物");
        suffixes.put("-or", "名词后缀，表示人或物");
        suffixes.put("-ist", "名词后缀，表示从事...的人");
        suffixes.put("-ize", "动词后缀，表示使成为");
        suffixes.put("-ify", "动词后缀，表示使成为");
        suffixes.put("-ous", "形容词后缀，表示具有...性质");
        suffixes.put("-ive", "形容词后缀，表示倾向于");
        
        for (Map.Entry<String, String> entry : prefixes.entrySet()) {
            if (word.startsWith(entry.getKey())) {
                sb.append("前缀 \"").append(entry.getKey()).append("\" 表示 \"").append(entry.getValue()).append("\"，");
                String root = word.substring(entry.getKey().length());
                sb.append("词根 \"").append(root).append("\" 可以联想为 ").append(definition).append("。");
                return sb.toString();
            }
        }
        
        for (Map.Entry<String, String> entry : suffixes.entrySet()) {
            if (word.endsWith(entry.getKey())) {
                sb.append("后缀 \"").append(entry.getKey()).append("\" 表示 \"").append(entry.getValue()).append("\"，");
                String root = word.substring(0, word.length() - entry.getKey().length());
                sb.append("词根 \"").append(root).append("\" 的含义是 ").append(definition).append("。");
                return sb.toString();
            }
        }
        
        // Simple letter association
        if (word.length() <= 6) {
            sb.append("观察字母组合，尝试将每个字母与词义联系起来：").append(definition);
        } else {
            String part1 = word.substring(0, word.length() / 2);
            String part2 = word.substring(word.length() / 2);
            sb.append("可以把单词分成两部分记忆：\"").append(part1).append("\" 和 \"").append(part2).append("\"，结合 ").append(definition).append(" 的含义。");
        }
        
        return sb.toString();
    }

    private List<Map<String, Object>> extractRoots(String word) {
        List<Map<String, Object>> roots = new ArrayList<>();
        if (word == null || word.length() < 4) {
            return roots;
        }
        
        Map<String, String> commonRoots = new LinkedHashMap<>();
        commonRoots.put("port", "携带，运送");
        commonRoots.put("dict", "说，言");
        commonRoots.put("duct", "引导，带领");
        commonRoots.put("spect", "看");
        commonRoots.put("scrib", "写");
        commonRoots.put("script", "写");
        commonRoots.put("ject", "投掷");
        commonRoots.put("tract", "拉，拖");
        commonRoots.put("struct", "建造");
        commonRoots.put("form", "形状，形式");
        commonRoots.put("mit", "发送");
        commonRoots.put("miss", "发送");
        commonRoots.put("vis", "看");
        commonRoots.put("vid", "看");
        commonRoots.put("aud", "听");
        commonRoots.put("cred", "相信");
        commonRoots.put("fac", "做，制作");
        commonRoots.put("fect", "做，制作");
        commonRoots.put("graph", "写，画");
        commonRoots.put("log", "说，学科");
        commonRoots.put("mem", "记忆");
        commonRoots.put("ment", "思想");
        commonRoots.put("nat", "出生");
        commonRoots.put("nov", "新");
        commonRoots.put("pos", "放置");
        commonRoots.put("rupt", "破裂");
        commonRoots.put("sens", "感觉");
        commonRoots.put("sent", "感觉");
        commonRoots.put("tend", "伸展");
        commonRoots.put("ten", "保持");
        commonRoots.put("ven", "来");
        commonRoots.put("vent", "来");
        commonRoots.put("vert", "转");
        commonRoots.put("vers", "转");
        
        for (Map.Entry<String, String> entry : commonRoots.entrySet()) {
            if (word.toLowerCase().contains(entry.getKey())) {
                roots.add(Map.of("part", entry.getKey(), "meaning", entry.getValue()));
            }
        }
        
        return roots;
    }

    private List<String> generateCollocations(String word, String pos) {
        List<String> collocations = new ArrayList<>();
        
        if (word == null) {
            collocations.add(word + " example");
            collocations.add(word + " usage");
            collocations.add(word + " context");
            return collocations;
        }
        
        String lowerPos = pos != null ? pos.toLowerCase() : "";
        
        if (lowerPos.contains("noun") || lowerPos.contains("名词")) {
            collocations.add("a " + word);
            collocations.add("the " + word);
            collocations.add("in " + word);
        } else if (lowerPos.contains("verb") || lowerPos.contains("动词")) {
            collocations.add("to " + word);
            collocations.add(word + " something");
            collocations.add(word + " quickly");
        } else if (lowerPos.contains("adj") || lowerPos.contains("形容词")) {
            collocations.add("very " + word);
            collocations.add("more " + word);
            collocations.add("the " + word + " thing");
        } else {
            collocations.add(word + " analysis");
            collocations.add(word + " study");
            collocations.add(word + " practice");
        }
        
        return collocations;
    }

    /**
     * Parses example sentences from the JSON string stored in Word.exampleSentences.
     * Expected format: [{"en":"...","cn":"..."}, ...]
     */
    private List<Map<String, Object>> parseExamples(String exampleJson) {
        if (exampleJson == null || exampleJson.isBlank()) {
            return List.of();
        }
        try {
            List<Map<String, Object>> examples = objectMapper.readValue(
                    exampleJson, new TypeReference<List<Map<String, Object>>>() {});
            // Ensure each entry has en, cn, context
            for (Map<String, Object> ex : examples) {
                ex.putIfAbsent("en", "");
                ex.putIfAbsent("cn", "");
                ex.putIfAbsent("context", "general");
            }
            return examples;
        } catch (Exception e) {
            log.debug("Could not parse example sentences JSON: {}", e.getMessage());
            // Attempt to treat as a plain comma-separated list
            return Arrays.stream(exampleJson.split("[,;]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("en", s);
                        m.put("cn", "");
                        m.put("context", "general");
                        return m;
                    })
                    .collect(Collectors.toList());
        }
    }

    /**
     * Parses synonyms from the JSON string stored in Word.synonyms.
     * Expected format: ["synonym1", "synonym2", ...]
     */
    private List<Map<String, Object>> parseSynonyms(String synonymsJson) {
        if (synonymsJson == null || synonymsJson.isBlank()) {
            return List.of();
        }
        try {
            List<String> synList = objectMapper.readValue(
                    synonymsJson, new TypeReference<List<String>>() {});
            return synList.stream()
                    .map(s -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("word", s);
                        m.put("diff", "");
                        return m;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.debug("Could not parse synonyms JSON: {}", e.getMessage());
            return Arrays.stream(synonymsJson.split("[,;\\[\\]\"]"))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(s -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("word", s);
                        m.put("diff", "");
                        return m;
                    })
                    .collect(Collectors.toList());
        }
    }

    // ------------------------------------------------ Fallback: image prompt
    /**
     * Builds a basic image prompt from word data when LLM is unavailable.
     */
    private String buildFallbackImagePrompt(Word word) {
        String definition = word.getDefinitionEn() != null ? word.getDefinitionEn() : word.getWord();
        return String.format(
                "A vivid, photorealistic scene illustrating the concept of '%s' (%s). "
                + "Bright colours, clear composition, educational style. No text in image.",
                word.getWord(), definition);
    }

    // ------------------------------------------------ Fallback: quiz
    /**
     * Generates a basic deterministic quiz when LLM is unavailable.
     */
    private Map<String, Object> _fallbackQuiz(List<Word> words) {
        List<Map<String, Object>> questions = new ArrayList<>();
        int qId = 1;

        for (Word word : words) {
            // 1) Choice question — meaning
            Map<String, Object> choice = new LinkedHashMap<>();
            choice.put("id", qId++);
            choice.put("type", "choice");
            choice.put("difficulty", word.getDifficulty() != null ? word.getDifficulty() : 3);
            choice.put("question", "What is the meaning of \"" + word.getWord() + "\"?");
            choice.put("options", List.of(
                    "A. " + safe(word.getDefinitionEn()),
                    "B. (distractor) to move quickly from one place to another",
                    "C. (distractor) a type of food or drink",
                    "D. (distractor) to make something smaller in size"
            ));
            choice.put("answer", "A");
            choice.put("explanation", "正确释义: " + safe(word.getDefinitionCn()));
            questions.add(choice);

            // 2) Fill-in-the-blank
            Map<String, Object> fill = new LinkedHashMap<>();
            fill.put("id", qId++);
            fill.put("type", "fill_blank");
            fill.put("difficulty", word.getDifficulty() != null ? word.getDifficulty() : 3);
            fill.put("question", "Fill in the blank with the correct form of \""
                    + word.getWord() + "\":\nThe student tried to ______ the concept through daily practice.");
            fill.put("options", List.of());
            fill.put("answer", word.getWord());
            fill.put("explanation", "此处应填入 " + word.getWord() + "，意为 " + safe(word.getDefinitionCn()) + "。");
            questions.add(fill);
        }

        Map<String, Object> quiz = new LinkedHashMap<>();
        quiz.put("quiz_title", "Vocabulary Quiz (Basic)");
        quiz.put("questions", questions);
        return quiz;
    }
}
