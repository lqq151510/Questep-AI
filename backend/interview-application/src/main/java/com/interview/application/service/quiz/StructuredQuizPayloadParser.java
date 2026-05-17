package com.interview.application.service.quiz;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class StructuredQuizPayloadParser {
    private static final Logger logger = LoggerFactory.getLogger(StructuredQuizPayloadParser.class);

    private final ObjectMapper objectMapper;

    public StructuredQuizPayloadParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public StructuredQuizPayload parse(String rawLlmResponse, int expectedCount) {
        if (rawLlmResponse == null || rawLlmResponse.isBlank()) {
            logger.warn("LLM returned empty content for structured quiz generation.");
            return null;
        }
        try {
            String json = sanitizeJsonPayload(rawLlmResponse);
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                logger.warn("LLM structured output schema mismatch: root is not object.");
                return null;
            }

            String summary = normalizeText(root.path("summary").asText(null));
            if (summary == null) {
                logger.warn("LLM structured output schema mismatch: summary missing.");
                return null;
            }

            JsonNode questionsNode = root.path("questions");
            if (!questionsNode.isArray() || questionsNode.isEmpty()) {
                logger.warn("LLM structured output schema mismatch: questions missing.");
                return null;
            }

            List<QuestionDraft> drafts = new ArrayList<>();
            for (JsonNode node : questionsNode) {
                String stem = normalizeText(node.path("stem").asText(null));
                String optionsJson = node.hasNonNull("optionsJson") ? node.path("optionsJson").toString() : null;
                String referenceAnswer = normalizeText(node.path("referenceAnswer").asText(null));
                String analysis = normalizeText(node.path("analysis").asText(null));
                if (stem == null || referenceAnswer == null || analysis == null) {
                    continue;
                }
                drafts.add(new QuestionDraft(stem, optionsJson, referenceAnswer, analysis));
                if (drafts.size() >= expectedCount) {
                    break;
                }
            }

            if (drafts.isEmpty()) {
                logger.warn("LLM structured output schema mismatch: no valid question items.");
                return null;
            }

            return new StructuredQuizPayload(summary, drafts);
        } catch (JsonProcessingException ex) {
            logger.warn("LLM structured output is not valid JSON: {}", ex.getOriginalMessage());
            return null;
        }
    }

    static String sanitizeJsonPayload(String raw) {
        String cleaned = raw.trim();
        // Strip markdown code fence if present
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline != -1) {
                cleaned = cleaned.substring(firstNewline + 1);
            }
            int endFence = cleaned.lastIndexOf("```");
            if (endFence != -1) {
                cleaned = cleaned.substring(0, endFence);
            }
        }
        cleaned = cleaned.trim();
        // Extract the first JSON object
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start != -1 && end > start) {
            cleaned = cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    private String normalizeText(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
