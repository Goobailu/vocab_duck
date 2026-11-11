package com.vocab.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocab.entity.User;
import com.vocab.entity.UserWord;
import com.vocab.entity.Word;
import com.vocab.repository.UserWordRepository;
import com.vocab.repository.WordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * CoordinatorAgent - Central orchestrator of the multi-agent vocabulary learning system.
 * <p>
 * Receives raw user messages, classifies intent, and delegates to the appropriate
 * specialist agent (diagnosis, content generation, review scheduling) to fulfil the
 * request.  All inter-agent communication happens through this class.
 */
@Component
public class CoordinatorAgent extends BaseAgent {
    private final Logger log = LoggerFactory.getLogger(getClass());

    // ── Intent keyword dictionaries (Chinese) ──────────────────────────────────

    private static final Map<String, List<String>> INTENT_KEYWORDS = new LinkedHashMap<>();

    static {
        INTENT_KEYWORDS.put("learn_new", Arrays.asList(
                "学习", "新词", "新单词", "开始学习", "背单词", "今天学", "教我"));
        INTENT_KEYWORDS.put("review", Arrays.asList(
                "复习", "回顾", "巩固", "该复习", "今日复习"));
        INTENT_KEYWORDS.put("explain", Arrays.asList(
                "解释", "什么意思", "怎么记", "记忆方法", "例句", "用法",
                "词根", "词源", "造句"));
        INTENT_KEYWORDS.put("quiz", Arrays.asList(
                "测试", "测验", "考考我", "练习", "做题", "出题"));
        INTENT_KEYWORDS.put("schedule", Arrays.asList(
                "计划", "安排", "日程", "什么时候复习", "学习规划", "学习计划", "复习计划"));
        INTENT_KEYWORDS.put("image", Arrays.asList(
                "图片", "画图", "配图", "图像", "画"));
        INTENT_KEYWORDS.put("chat", Arrays.asList(
                "你好", "hello", "hi", "帮助", "你是谁", "怎么用"));
    }

    /** Pattern that captures text inside Chinese/English quotation marks. */
    private static final Pattern QUOTED_WORD =
            Pattern.compile("[\"\\u201c\\u201d\\u300c\\u300d'\\u2018\\u2019]([A-Za-z\\s\\-]+)[\"\\u201c\\u201d\\u300c\\u300d'\\u2018\\u2019]");

    /** Pattern that captures any ASCII-only token sequence (fallback). */
    private static final Pattern ASCII_WORD =
            Pattern.compile("\\b([A-Za-z][A-Za-z\\-]{1,})\\b");

    // ── Injected collaborators ─────────────────────────────────────────────────

    @Autowired
    private ContentGenerationAgent contentAgent;

    @Autowired
    private ReviewSchedulingAgent schedulingAgent;

    @Autowired
    private UserWordRepository userWordRepository;

    @Autowired
    private WordRepository wordRepository;

    // ── BaseAgent contract ─────────────────────────────────────────────────────

    @Override
    public String getName() {
        return "coordinator";
    }

    // ── Public API ─────────────────────────────────────────────────────────────

    /**
     * Classify the user's natural-language message into an intent bucket.
     *
     * @param message raw user input (Chinese / English mixed)
     * @return map with keys: intent, confidence, targetWord
     */
    public Map<String, Object> classifyIntent(String message) {
        if (message == null || message.isBlank()) {
            return buildIntentResult("chat", 0, null);
        }

        String lower = message.toLowerCase();

        String bestIntent = "chat";
        int bestScore = 0;

        for (Map.Entry<String, List<String>> entry : INTENT_KEYWORDS.entrySet()) {
            int score = 0;
            for (String keyword : entry.getValue()) {
                if (lower.contains(keyword.toLowerCase())) {
                    score++;
                }
            }
            if (score > bestScore) {
                bestScore = score;
                bestIntent = entry.getKey();
            }
        }

        // Confidence = score / 3, capped at 1.0
        double confidence = Math.min(1.0, bestScore / 3.0);
        if (confidence <= 0.0 && "chat".equals(bestIntent)) {
            // No keyword matched at all – low-confidence fallback
            confidence = 0.1;
        }

        String targetWord = extractTargetWord(message);

        log.info("Intent classified: intent={}, confidence={}, targetWord={}", bestIntent, confidence, targetWord);
        return buildIntentResult(bestIntent, confidence, targetWord);
    }

    /**
     * Main entry point – processes a user message end-to-end.
     *
     * @param user    the authenticated user
     * @param message raw natural-language message
     * @return structured result map with reply, agentsInvoked, cards, intent, data
     */
    public Map<String, Object> handleMessage(User user, String message) {
        Map<String, Object> intentResult = classifyIntent(message);
        String intent = (String) intentResult.get("intent");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("reply", "");
        result.put("agentsInvoked", new ArrayList<String>());
        result.put("cards", new ArrayList<Map<String, Object>>());
        result.put("intent", intentResult);
        result.put("data", new LinkedHashMap<String, Object>());

        switch (intent) {
            case "learn_new":
                handleLearnNew(user, result);
                break;
            case "review":
                handleReview(user, result);
                break;
            case "explain":
                handleExplain(user, message, result);
                break;
            case "quiz":
                handleQuiz(user, result);
                break;
            case "schedule":
                handleSchedule(user, result);
                break;
            case "image":
                handleImage(user, message, result);
                break;
            case "chat":
            default:
                handleChat(user, message, result);
                break;
        }

        return result;
    }

    // ── Handler methods ────────────────────────────────────────────────────────

    /**
     * Runs a full memory diagnosis for the user and produces summary cards.
     */
    /**
     * Fetches new words from the scheduling agent, generates learning content
     * cards, and creates UserWord entries for tracking.
     */
    private void handleLearnNew(User user, Map<String, Object> result) {
        List<String> agents = agentsInvoked(result);
        List<Map<String, Object>> cards = cards(result);

        agents.add("coordinator");
        agents.add("review-scheduling");
        agents.add("content-generation");

        // Get new words from scheduling agent
        List<Map<String, Object>> scheduledWords = schedulingAgent.getNewWords(user, 10);

        if (scheduledWords == null || scheduledWords.isEmpty()) {
            result.put("reply", "目前没有可学习的新单词了。你可以试试复习已学过的单词，或者调整学习计划添加新词库。");
            return;
        }

        StringBuilder replyBuilder = new StringBuilder();
        replyBuilder.append("好的，今天为你准备了 ").append(scheduledWords.size()).append(" 个新单词：\n\n");

        for (Map<String, Object> wordInfo : scheduledWords) {
            String wordText = (String) wordInfo.getOrDefault("word", "");
            Long wordId = wordInfo.get("wordId") != null
                    ? ((Number) wordInfo.get("wordId")).longValue()
                    : null;

            // Create UserWord entry for tracking
            if (wordId != null) {
                java.util.Optional<UserWord> existingOpt = userWordRepository.findByUserIdAndWordId(user.getId(), wordId);
                if (existingOpt.isEmpty()) {
                    UserWord newUserWord = UserWord.builder()
                            .userId(user.getId())
                            .wordId(wordId)
                            .status("learning")
                            .masteryLevel(0.1)
                            .repetitions(0)
                            .intervalDays(1)
                            .easeFactor(2.5)
                            .nextReview(java.time.LocalDateTime.now().plusDays(1))
                            .learningQueue("learning")
                            .build();
                    userWordRepository.save(newUserWord);
                    log.info("Created UserWord for user={}, word={}", user.getUsername(), wordText);
                }
            }

            // Generate rich content card via content agent
            Map<String, Object> card = contentAgent.generateWordCard(user, wordText);
            if (card != null && !card.isEmpty()) {
                card.put("type", "word_card");
                cards.add(card);
            }

            replyBuilder.append("- ").append(wordText);
            if (wordInfo.containsKey("definition")) {
                replyBuilder.append(": ").append(wordInfo.get("definition"));
            } else if (wordInfo.containsKey("definitionCn")) {
                replyBuilder.append(": ").append(wordInfo.get("definitionCn"));
            }
            replyBuilder.append("\n");
        }

        result.put("reply", replyBuilder.toString().trim());
        data(result).put("wordCount", scheduledWords.size());
        data(result).put("words", scheduledWords);
    }

    /**
     * Retrieves due words for review and generates review cards.
     */
    private void handleReview(User user, Map<String, Object> result) {
        List<String> agents = agentsInvoked(result);
        List<Map<String, Object>> cards = cards(result);

        agents.add("coordinator");
        agents.add("review-scheduling");
        agents.add("content-generation");

        List<Map<String, Object>> dueWords = schedulingAgent.getDueWords(user, 20);

        if (dueWords == null || dueWords.isEmpty()) {
            result.put("reply", "太棒了！目前没有需要复习的单词。你可以继续学习新词，或者稍后再来复习。");
            return;
        }

        StringBuilder replyBuilder = new StringBuilder();
        replyBuilder.append("你有 ").append(dueWords.size()).append(" 个单词需要复习：\n\n");

        for (Map<String, Object> wordInfo : dueWords) {
            String wordText = (String) wordInfo.getOrDefault("word", "");

            Map<String, Object> reviewCard = null;
            try {
                reviewCard = contentAgent.generateReviewCard(user, wordText);
            } catch (Exception e) {
                log.warn("Failed to generate review card for word '{}': {}", wordText, e.getMessage());
            }
            
            if (reviewCard != null && !reviewCard.isEmpty() && !"error".equals(reviewCard.get("type"))) {
                reviewCard.put("type", "review_card");
                reviewCard.put("masteryLevel", wordInfo.get("masteryLevel"));
                reviewCard.put("priority", wordInfo.get("priority"));
                cards.add(reviewCard);
            } else if (!wordText.isBlank()) {
                Map<String, Object> simpleCard = new LinkedHashMap<>();
                simpleCard.put("type", "review_card");
                simpleCard.put("word", wordText);
                simpleCard.put("phonetic", wordInfo.get("phonetic"));
                simpleCard.put("masteryLevel", wordInfo.get("masteryLevel"));
                simpleCard.put("priority", wordInfo.get("priority"));
                
                List<Map<String, Object>> definitions = new ArrayList<>();
                String defCn = (String) wordInfo.getOrDefault("definition_cn", "");
                String defEn = (String) wordInfo.getOrDefault("definition_en", "");
                
                if (defCn != null && !defCn.isBlank()) {
                    String[] cnParts = defCn.split("[,;，；]");
                    String[] enParts = defEn != null ? defEn.split("[,;，；]") : new String[]{};
                    
                    for (int i = 0; i < cnParts.length; i++) {
                        Map<String, Object> def = new LinkedHashMap<>();
                        def.put("pos", "unknown");
                        def.put("en", i < enParts.length ? enParts[i].trim() : defEn);
                        def.put("cn", cnParts[i].trim());
                        definitions.add(def);
                    }
                } else {
                    Map<String, Object> def = new LinkedHashMap<>();
                    def.put("pos", "unknown");
                    def.put("en", defEn != null ? defEn : "");
                    def.put("cn", defCn != null ? defCn : "");
                    definitions.add(def);
                }
                simpleCard.put("definitions", definitions);
                
                String wordLower = wordText.toLowerCase();
                Map<String, Object> mnemonic = new LinkedHashMap<>();
                mnemonic.put("method", "联想记忆");
                mnemonic.put("content", "单词 \"" + wordText + "\" 可以通过多读多写来加深记忆，结合例句理解其用法。");
                mnemonic.put("visual_hint", "Imagine a scene related to '" + wordText + "'");
                simpleCard.put("mnemonic", mnemonic);
                
                List<Map<String, Object>> examples = new ArrayList<>();
                examples.add(Map.of("en", "This is an example of using the word: " + wordText + ".", "cn", "这是使用单词 " + wordText + " 的例子。", "context", "general"));
                simpleCard.put("examples", examples);
                
                List<String> collocations = new ArrayList<>();
                collocations.add("to " + wordLower);
                collocations.add(wordLower + " something");
                simpleCard.put("collocations", collocations);
                
                simpleCard.put("exam_tips", "此单词需要重点复习，请结合释义和例句理解记忆。");
                
                cards.add(simpleCard);
            }

            replyBuilder.append("- ").append(wordText);
            replyBuilder.append("\n");
        }

        result.put("reply", replyBuilder.toString().trim());
        data(result).put("dueCount", dueWords.size());
        data(result).put("dueWords", dueWords);
    }

    /**
     * Explains a specific word – extracts the target word from the message and
     * generates a detailed content card.
     */
    private void handleExplain(User user, String message, Map<String, Object> result) {
        List<String> agents = agentsInvoked(result);
        List<Map<String, Object>> cards = cards(result);

        agents.add("coordinator");
        agents.add("content-generation");

        String targetWord = extractTargetWord(message);

        if (targetWord == null || targetWord.isBlank()) {
            result.put("reply", "请告诉我你想了解哪个单词？你可以说：解释 \"example\" 或者 解释 example。");
            return;
        }

        Map<String, Object> card = contentAgent.generateWordCard(user, targetWord);
        if (card != null) {
            String cardType = (String) card.get("type");
            if ("error".equals(cardType)) {
                String errorMsg = (String) card.get("message");
                result.put("reply", errorMsg != null ? errorMsg : "抱歉，处理单词解释时出现错误。");
                return;
            }
            card.put("type", "word_card");
            cards.add(card);
        }

        String reply = buildReply(
                "好的，来为你详细解释 \"" + targetWord + "\" 这个词。",
                "包含释义、例句、词根词源、记忆方法和用法搭配。"
        );
        result.put("reply", reply);
        data(result).put("targetWord", targetWord);
    }

    /**
     * Generates a quiz from the user's recently learned words.
     */
    private void handleQuiz(User user, Map<String, Object> result) {
        List<String> agents = agentsInvoked(result);
        List<Map<String, Object>> cards = cards(result);

        agents.add("coordinator");
        agents.add("content-generation");

        List<UserWord> recentWords = userWordRepository
                .findByUserIdOrderByLastReviewedDesc(user.getId());

        log.info("handleQuiz: recentWords count = {}", recentWords != null ? recentWords.size() : 0);

        if (recentWords == null || recentWords.isEmpty()) {
            result.put("reply", "你还没有学习过任何单词，先学习一些新词再来测验吧！");
            return;
        }

        List<UserWord> quizPool = recentWords.stream().limit(10).collect(Collectors.toList());
        List<Word> quizWords = quizPool.stream()
                .map(UserWord::getWord)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("handleQuiz: quizWords count = {}", quizWords.size());

        Map<String, Object> quizCard = contentAgent.generateQuiz(user, quizWords);
        log.info("handleQuiz: quizCard is null = {}, cards size = {}", quizCard == null, cards.size());

        if (quizCard != null) {
            quizCard.put("type", "quiz");
            cards.add(quizCard);
            log.info("handleQuiz: added quiz card, cards size = {}", cards.size());
        }

        List<String> wordTexts = quizWords.stream()
                .map(Word::getWord)
                .collect(Collectors.toList());

        result.put("reply", "好的，我根据你最近学习的 " + wordTexts.size() + " 个单词出了一组测验题，来试试吧！");
        data(result).put("quizWordCount", wordTexts.size());
        data(result).put("quizWords", wordTexts);

        log.info("handleQuiz: final reply = {}, cards size = {}", result.get("reply"), cards.size());
    }

    /**
     * Shows the user's review schedule overview.
     */
    private void handleSchedule(User user, Map<String, Object> result) {
        List<String> agents = agentsInvoked(result);
        List<Map<String, Object>> cards = cards(result);

        agents.add("coordinator");
        agents.add("review-scheduling");

        Map<String, Object> dailyPlan = schedulingAgent.generateDailyPlan(user, 60);

        Map<String, Object> scheduleCard = new LinkedHashMap<>();
        scheduleCard.put("type", "schedule");
        scheduleCard.put("title", "今日计划");
        scheduleCard.put("tasks", dailyPlan.get("tasks"));
        cards.add(scheduleCard);

        List<Map<String, Object>> tasks = (List<Map<String, Object>>) dailyPlan.get("tasks");
        StringBuilder replyBuilder = new StringBuilder("这是你今天的学习计划：\n\n");
        
        if (tasks != null && !tasks.isEmpty()) {
            int index = 1;
            for (Map<String, Object> task : tasks) {
                String taskName = (String) task.get("taskName");
                String priority = (String) task.get("priority");
                Integer durationMinutes = (Integer) task.get("durationMinutes");
                Integer wordCount = (Integer) task.get("wordCount");
                
                String priorityLabel = "普通";
                if ("high".equals(priority)) priorityLabel = "优先";
                else if ("low".equals(priority)) priorityLabel = "次要";
                
                replyBuilder.append(index++).append(". ")
                        .append(priorityLabel).append("：")
                        .append(taskName);
                
                if (wordCount != null && wordCount > 0) {
                    replyBuilder.append("（").append(wordCount).append("个单词）");
                }
                if (durationMinutes != null && durationMinutes > 0) {
                    replyBuilder.append("，预计").append(durationMinutes).append("分钟");
                }
                replyBuilder.append("\n");
            }
        } else {
            replyBuilder.append("今天没有安排学习任务，可以自由学习！");
        }
        
        result.put("reply", replyBuilder.toString());
        data(result).putAll(dailyPlan);
    }

    /**
     * Extracts a word from the message and asks the content agent to generate
     * an image prompt for visual memory aids.
     */
    private void handleImage(User user, String message, Map<String, Object> result) {
        List<String> agents = agentsInvoked(result);
        List<Map<String, Object>> cards = cards(result);

        agents.add("coordinator");
        agents.add("content-generation");

        String targetWord = extractTargetWord(message);

        if (targetWord == null || targetWord.isBlank()) {
            result.put("reply", "请告诉我你想为哪个单词生成配图？例如：为 \"serendipity\" 画图。");
            return;
        }

        Optional<Word> wordOpt = wordRepository.findByWord(targetWord);
        if (wordOpt.isEmpty()) {
            result.put("reply", "抱歉，我找不到单词 \"" + targetWord + "\"。请尝试其他单词。");
            return;
        }
        Word word = wordOpt.get();

        Map<String, Object> imageResult = contentAgent.generateWordImage(user, word, "1024x1024");

        Map<String, Object> imageCard = new LinkedHashMap<>();
        imageCard.put("type", "image");
        imageCard.put("word", targetWord);
        imageCard.put("imageLoading", false);

        if ("success".equals(imageResult.get("status")) && imageResult.containsKey("imageUrl")) {
            imageCard.put("imageUrl", imageResult.get("imageUrl"));
            imageCard.put("prompt", imageResult.get("prompt"));
            result.put("reply", "我为 \"" + targetWord + "\" 生成了一张记忆配图。");
        } else {
            imageCard.put("imageUrl", "");
            String errorMsg = (String) imageResult.getOrDefault("message", "图像生成失败");
            result.put("reply", "抱歉，为 \"" + targetWord + "\" 生成配图时遇到问题：" + errorMsg);
        }

        cards.add(imageCard);
        data(result).put("targetWord", targetWord);
        data(result).put("imageResult", imageResult);
    }

    /**
     * General chat / help handler – responds to greetings and usage questions.
     */
    private void handleChat(User user, String message, Map<String, Object> result) {
        List<String> agents = agentsInvoked(result);
        agents.add("coordinator");

        String reply = defaultHelpMessage();
        result.put("reply", reply);
    }

    // ── Helper methods ─────────────────────────────────────────────────────────

    /**
     * Extracts the target word from the user message.  First tries to find a
     * word inside quotation marks, then falls back to the first ASCII token.
     *
     * @param message raw user input
     * @return extracted word, or {@code null} if none found
     */
    String extractTargetWord(String message) {
        if (message == null) {
            return null;
        }

        // Strategy 1: quoted text
        Matcher quotedMatcher = QUOTED_WORD.matcher(message);
        if (quotedMatcher.find()) {
            return quotedMatcher.group(1).trim();
        }

        // Strategy 2: first ASCII word-like token (at least 2 chars)
        Matcher asciiMatcher = ASCII_WORD.matcher(message);
        while (asciiMatcher.find()) {
            String candidate = asciiMatcher.group(1).trim();
            // Skip common noise words
            if (!isNoiseWord(candidate)) {
                return candidate;
            }
        }

        return null;
    }

    /**
     * Returns a default Chinese-language help message listing all capabilities.
     */
    String defaultHelpMessage() {
        return "你好！我是你的智能词汇学习助手，可以帮你：\n\n"
                + "1. 学习新词 — 说\"学习\"或\"背单词\"开始学习新单词\n"
                + "2. 复习巩固 — 说\"复习\"查看需要复习的单词\n"
                + "3. 单词解释 — 说\"解释 example\"获取详细释义和例句\n"
                + "4. 记忆诊断 — 说\"诊断\"或\"学习报告\"查看你的学习分析\n"
                + "5. 测验练习 — 说\"测试\"或\"考考我\"进行单词测验\n"
                + "6. 学习计划 — 说\"计划\"查看复习安排\n"
                + "7. 学习建议 — 说\"建议\"获取个性化学习建议\n"
                + "8. 配图辅助 — 说\"为 xxx 画图\"生成记忆配图\n\n"
                + "你可以直接用中文告诉我你想做什么，我会理解你的意思并帮你完成！";
    }

    // ── Private utilities ──────────────────────────────────────────────────────

    private Map<String, Object> buildIntentResult(String intent, double confidence, String targetWord) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("intent", intent);
        map.put("confidence", confidence);
        map.put("targetWord", targetWord);
        return map;
    }

    private String buildReply(String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part != null && !part.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append("\n");
                }
                sb.append(part);
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private List<String> agentsInvoked(Map<String, Object> result) {
        return (List<String>) result.get("agentsInvoked");
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> cards(Map<String, Object> result) {
        return (List<Map<String, Object>>) result.get("cards");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> data(Map<String, Object> result) {
        return (Map<String, Object>) result.get("data");
    }

    private boolean isNoiseWord(String word) {
        Set<String> noise = Set.of(
                "the", "is", "at", "it", "on", "in", "to", "of", "an", "as",
                "be", "by", "or", "do", "if", "me", "up", "so", "no", "he",
                "we", "my", "am", "can", "has", "her", "was", "one", "all",
                "are", "and", "for", "not", "but", "had", "you", "his", "she",
                "they", "that", "this", "with", "have", "from", "what", "when",
                "how", "which", "where"
        );
        return noise.contains(word.toLowerCase());
    }
}
