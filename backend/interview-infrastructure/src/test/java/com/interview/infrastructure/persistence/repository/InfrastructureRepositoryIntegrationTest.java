package com.interview.infrastructure.persistence.repository;

import com.interview.domain.model.AsyncTaskRecord;
import com.interview.domain.model.Material;
import com.interview.domain.model.Question;
import com.interview.domain.model.User;
import com.interview.domain.repository.AsyncTaskRecordRepository;
import com.interview.domain.repository.MaterialRepository;
import com.interview.domain.repository.QuestionRepository;
import com.interview.domain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(
        classes = InfrastructureRepositoryIntegrationTest.TestConfiguration.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:infra_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;CASE_INSENSITIVE_IDENTIFIERS=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.sql.init.mode=always",
                "spring.sql.init.schema-locations=classpath:schema.sql",
                "mybatis.mapper-locations=classpath*:mapper/*.xml",
                "mybatis.configuration.map-underscore-to-camel-case=true"
        }
)
class InfrastructureRepositoryIntegrationTest {
    private static final String REVIEW_STATUS_APPROVED = "APPROVED";
    private static final String REVIEW_STATUS_PENDING = "PENDING_REVIEW";

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @MapperScan("com.interview.infrastructure.persistence.mapper")
    @ComponentScan(basePackageClasses = {
            UserRepositoryImpl.class,
            MaterialRepositoryImpl.class,
            AsyncTaskRecordRepositoryImpl.class,
            QuestionRepositoryImpl.class
    })
    static class TestConfiguration {
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MaterialRepository materialRepository;

    @Autowired
    private AsyncTaskRecordRepository asyncTaskRecordRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTables() {
        jdbcTemplate.update("DELETE FROM questions");
        jdbcTemplate.update("DELETE FROM async_task_records");
        jdbcTemplate.update("DELETE FROM materials");
        jdbcTemplate.update("DELETE FROM users");
    }

    @Test
    void userRepositoryShouldSaveAndQueryUser() {
        User saved = userRepository.save("alice", "alice@example.com", "pwd_hash");

        assertNotNull(saved.id());
        assertEquals("alice", saved.username());
        assertTrue(userRepository.findByUsername("alice").isPresent());
        assertTrue(userRepository.findByEmail("alice@example.com").isPresent());
        assertTrue(userRepository.findById(saved.id()).isPresent());
    }

    @Test
    void materialRepositoryShouldTrackParseLifecycle() {
        User user = userRepository.save("bob", "bob@example.com", "pwd_hash");
        Material material = materialRepository.save(user.id(), "java-notes.md", "md", "/tmp/java-notes.md");

        assertEquals("PENDING", material.parseStatus());

        materialRepository.markParseSuccess(material.id(), "hash-1", "analysis");
        Material success = materialRepository.findById(material.id()).orElseThrow();
        assertEquals("SUCCESS", success.parseStatus());
        assertEquals("hash-1", success.contentHash());
        assertEquals("analysis", success.analysisText());

        materialRepository.markParseFailure(material.id(), "x".repeat(700));
        Material failed = materialRepository.findById(material.id()).orElseThrow();
        assertEquals("FAILED", failed.parseStatus());
        assertNotNull(failed.parseErrorMsg());
        assertEquals(500, failed.parseErrorMsg().length());
    }

    @Test
    void asyncTaskRecordRepositoryShouldSupportStatusUpdates() {
        User user = userRepository.save("charlie", "charlie@example.com", "pwd_hash");
        AsyncTaskRecord created = asyncTaskRecordRepository.create("TASK-001", "MATERIAL_PARSE", 101L, user.id());

        assertEquals("PENDING", created.status());
        assertEquals(0, created.progress());

        AsyncTaskRecord processing = asyncTaskRecordRepository.updateStatus(created.id(), "PROCESSING", 35);
        assertEquals("PROCESSING", processing.status());
        assertEquals(35, processing.progress());

        AsyncTaskRecord failed = asyncTaskRecordRepository.updateError(created.id(), "parse failed", null, null, null);
        assertEquals("FAILED", failed.status());
        assertEquals("parse failed", failed.errorMsg());
        assertNull(failed.errorCode());
        assertNull(failed.stage());
        assertNull(failed.retryable());
        assertTrue(asyncTaskRecordRepository.findByTaskNo("TASK-001").isPresent());
    }

    @Test
    void questionRepositoryShouldReturnRecentQuestionsInDescOrder() {
        User user = userRepository.save("diana", "diana@example.com", "pwd_hash");
        Material material = materialRepository.save(user.id(), "spring-notes.md", "md", "/tmp/spring-notes.md");

        Question first = questionRepository.save(
                material.id(),
                user.id(),
                "short",
                "What is transaction propagation?",
                "Answer 1",
                null,
                2,
                "manual",
                "test-model"
        );
        Question second = questionRepository.save(
                material.id(),
                user.id(),
                "short",
                "How does optimistic locking work?",
                "Answer 2",
                null,
                3,
                "manual",
                "test-model"
        );

        List<Question> recent = questionRepository.findRecentByUser(user.id(), 0, 10);
        assertEquals(2, recent.size());
        assertEquals(second.id(), recent.get(0).id());
        assertEquals(first.id(), recent.get(1).id());
        assertEquals("material://" + second.materialId(), second.sourceUrl());
        assertEquals("material-v1", second.sourceVersion());
        assertEquals(REVIEW_STATUS_APPROVED, second.reviewStatus());
        assertNotNull(second.expiresAt());
    }

    @Test
    void questionRepositoryShouldMarkExpiredQuestionsForReview() {
        User user = userRepository.save("eva", "eva@example.com", "pwd_hash");
        Material material = materialRepository.save(user.id(), "redis-notes.md", "md", "/tmp/redis-notes.md");
        Question saved = questionRepository.save(
                material.id(),
                user.id(),
                "short",
                "Redis 持久化策略区别？",
                "AOF + RDB",
                null,
                3,
                "manual",
                "test-model"
        );

        int affected = questionRepository.markExpiredForReview(
                LocalDateTime.now().plusDays(31),
                REVIEW_STATUS_PENDING
        );
        assertTrue(affected >= 1);

        Question updated = questionRepository.findById(saved.id()).orElseThrow();
        assertEquals(REVIEW_STATUS_PENDING, updated.reviewStatus());
    }

    @Test
    void questionRepositoryShouldExcludePendingAndArchivedFromRecentList() {
        User user = userRepository.save("frank", "frank@example.com", "pwd_hash");
        Material material = materialRepository.save(user.id(), "spring-cache.md", "md", "/tmp/spring-cache.md");

        Question approved = questionRepository.save(
                material.id(), user.id(), "short", "A", "A1", null, 3, "manual", "test-model"
        );
        Question pending = questionRepository.save(
                material.id(), user.id(), "short", "B", "B1", null, 3, "manual", "test-model"
        );
        Question archived = questionRepository.save(
                material.id(), user.id(), "short", "C", "C1", null, 3, "manual", "test-model"
        );

        jdbcTemplate.update("UPDATE questions SET review_status = 'PENDING_REVIEW' WHERE id = ?", pending.id());
        questionRepository.archiveQuestion(archived.id(), LocalDateTime.now());

        List<Question> recent = questionRepository.findRecentByUser(user.id(), 0, 20);
        assertEquals(1, recent.size());
        assertEquals(approved.id(), recent.getFirst().id());
    }

    @Test
    void questionRepositoryShouldProvidePendingRefreshCandidates() {
        User user = userRepository.save("grace", "grace@example.com", "pwd_hash");
        Material material = materialRepository.save(user.id(), "mq.md", "md", "/tmp/mq.md");
        Question saved = questionRepository.save(
                material.id(), user.id(), "short", "MQ", "A", null, 3, "manual", "test-model"
        );
        jdbcTemplate.update("UPDATE questions SET review_status = 'PENDING_REVIEW' WHERE id = ?", saved.id());

        List<Question> candidates = questionRepository.findPendingRefreshCandidates(10, REVIEW_STATUS_PENDING);
        assertEquals(1, candidates.size());
        assertEquals(saved.id(), candidates.getFirst().id());
    }
}
