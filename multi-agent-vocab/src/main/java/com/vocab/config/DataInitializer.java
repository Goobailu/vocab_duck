package com.vocab.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

/**
 * Auto-initializes seed vocabulary data on application startup.
 * Checks per-library tag counts to ensure all word banks are complete.
 * If any library has fewer than 50 words, reloads the library seed data.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    /** Minimum expected words per library tag — below this triggers a reload */
    private static final int MIN_WORDS_PER_LIBRARY = 50;

    /** Library tags that must each have sufficient word count */
    private static final String[] LIBRARY_TAGS = {"cet4", "cet6", "kaoyan", "ielts", "toefl"};

    @Autowired
    private DataSource dataSource;

    @Override
    public void run(String... args) {
        try (var conn = dataSource.getConnection()) {
            var stmt = conn.createStatement();
            var rs = stmt.executeQuery("SELECT COUNT(*) FROM words");
            rs.next();
            int totalCount = rs.getInt(1);
            rs.close();
            stmt.close();

            if (totalCount == 0) {
                // 数据库为空，加载全部种子数据
                log.info("Words table is empty. Loading all seed data...");
                ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/seed-data.sql"));
                log.info("Base seed data loaded. Loading library words...");
                ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/seed-libraries.sql"));
                log.info("All seed data loaded successfully.");
                return;
            }

            // 检查每个词库的单词数量是否足够
            boolean needsReload = false;
            var checkStmt = conn.createStatement();
            for (String tag : LIBRARY_TAGS) {
                var tagRs = checkStmt.executeQuery(
                        "SELECT COUNT(*) FROM words WHERE tags LIKE '%" + tag + "%'");
                tagRs.next();
                int tagCount = tagRs.getInt(1);
                tagRs.close();
                log.info("词库 [{}] 当前单词数: {}", tag, tagCount);
                if (tagCount < MIN_WORDS_PER_LIBRARY) {
                    needsReload = true;
                    log.warn("词库 [{}] 单词数量不足 ({} < {})，需要重新加载", tag, tagCount, MIN_WORDS_PER_LIBRARY);
                }
            }
            checkStmt.close();

            if (needsReload) {
                log.info("部分词库数据不完整，重新加载 library 种子数据...");
                ScriptUtils.executeSqlScript(conn, new ClassPathResource("db/seed-libraries.sql"));
                log.info("Library words reloaded successfully.");
            } else {
                log.info("所有词库数据完整 (总计 {} 条)，跳过种子数据加载。", totalCount);
            }
        } catch (Exception e) {
            log.warn("Could not initialize seed data: {}", e.getMessage());
        }
    }
}
