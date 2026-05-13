package com.interview.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.application.port.LlmGateway;
import com.interview.domain.model.EvalCase;
import com.interview.domain.model.EvalResult;
import com.interview.domain.model.EvalRun;
import com.interview.domain.repository.EvalCaseRepository;
import com.interview.domain.repository.EvalResultRepository;
import com.interview.domain.repository.EvalRunRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EvalService {

    private static final Logger log = LoggerFactory.getLogger(EvalService.class);

    private final LlmGateway llmGateway;
    private final EvalCaseRepository evalCaseRepository;
    private final EvalRunRepository evalRunRepository;
    private final EvalResultRepository evalResultRepository;
    private final ObjectMapper objectMapper;

    public EvalService(
            LlmGateway llmGateway,
            EvalCaseRepository evalCaseRepository,
            EvalRunRepository evalRunRepository,
            EvalResultRepository evalResultRepository,
            ObjectMapper objectMapper
    ) {
        this.llmGateway = llmGateway;
        this.evalCaseRepository = evalCaseRepository;
        this.evalRunRepository = evalRunRepository;
        this.evalResultRepository = evalResultRepository;
        this.objectMapper = objectMapper;
    }

    public EvalRun runEval(String runKey) {
        EvalRun run = evalRunRepository.save(new EvalRun(
                null, runKey, null, 0, 0, 0.0, "RUNNING", LocalDateTime.now(), null, null
        ));

        List<EvalCase> cases = evalCaseRepository.findActive();
        if (cases.isEmpty()) {
            evalRunRepository.updateFinished(run.id(), 0, 0, 0.0, "COMPLETED");
            return evalRunRepository.findByRunKey(runKey).orElseThrow();
        }

        int passed = 0;
        double totalScore = 0.0;
        List<String> errors = new ArrayList<>();

        for (EvalCase evalCase : cases) {
            try {
                long start = System.currentTimeMillis();
                String prompt = buildEvalPrompt(evalCase);
                String actualOutput = llmGateway.chat(prompt);
                long durationMs = System.currentTimeMillis() - start;

                double score = computeScore(actualOutput, evalCase.expectedKeywords());
                String keywordHitsJson = computeKeywordHits(actualOutput, evalCase.expectedKeywords());
                boolean passed_ = score >= evalCase.minScore();

                evalResultRepository.save(new EvalResult(
                        null, run.id(), evalCase.id(), actualOutput,
                        score, keywordHitsJson, null, durationMs, null, null
                ));

                totalScore += score;
                if (passed_) passed++;
            } catch (Exception e) {
                log.warn("Eval case {} failed: {}", evalCase.caseKey(), e.getMessage());
                evalResultRepository.save(new EvalResult(
                        null, run.id(), evalCase.id(), null,
                        0.0, "[]", null, 0L,
                        e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500)) : "unknown",
                        null
                ));
                errors.add(evalCase.caseKey() + ": " + e.getMessage());
            }
        }

        int totalCases = cases.size();
        double avgScore = totalCases > 0 ? totalScore / totalCases : 0.0;
        evalRunRepository.updateFinished(run.id(), totalCases, passed, Math.round(avgScore * 100.0) / 100.0, "COMPLETED");

        return evalRunRepository.findByRunKey(runKey).orElseThrow();
    }

    private String buildEvalPrompt(EvalCase evalCase) {
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> input = objectMapper.readValue(evalCase.input(), Map.class);
            if ("quiz".equals(evalCase.category())) {
                return buildQuizEvalPrompt(input);
            }
            return buildChatEvalPrompt(input);
        } catch (JsonProcessingException e) {
            return evalCase.input();
        }
    }

    private String buildChatEvalPrompt(Map<String, Object> input) {
        Object message = input.get("message");
        return "You are a technical interview assistant. Answer concisely.\n\nUser question: " + message;
    }

    private String buildQuizEvalPrompt(Map<String, Object> input) {
        Object questionType = input.getOrDefault("questionType", "INTERVIEW");
        Object difficulty = input.getOrDefault("difficulty", 3);
        Object count = input.getOrDefault("count", 2);
        Object materialNames = input.getOrDefault("materialNames", "[]");
        return "Generate " + count + " " + questionType + " questions. Materials=" + materialNames
                + ", difficulty=" + difficulty
                + ". Return ONLY strict JSON with schema: "
                + "{\"summary\":\"string\",\"questions\":[{\"stem\":\"string\",\"referenceAnswer\":\"string\",\"analysis\":\"string\"}]}.";
    }

    double computeScore(String actualOutput, String expectedKeywordsJson) {
        if (actualOutput == null || actualOutput.isEmpty()) {
            return 0.0;
        }
        List<String> keywords = parseKeywords(expectedKeywordsJson);
        if (keywords.isEmpty()) {
            return 1.0;
        }
        int hits = 0;
        String lowerOutput = actualOutput.toLowerCase();
        for (String keyword : keywords) {
            if (lowerOutput.contains(keyword.toLowerCase())) {
                hits++;
            }
        }
        return Math.round((double) hits / keywords.size() * 100.0) / 100.0;
    }

    private String computeKeywordHits(String actualOutput, String expectedKeywordsJson) {
        List<String> keywords = parseKeywords(expectedKeywordsJson);
        List<String> hitList = new ArrayList<>();
        if (actualOutput == null) {
            return "[]";
        }
        String lowerOutput = actualOutput.toLowerCase();
        for (String keyword : keywords) {
            if (lowerOutput.contains(keyword.toLowerCase())) {
                hitList.add(keyword);
            }
        }
        try {
            return objectMapper.writeValueAsString(hitList);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private List<String> parseKeywords(String expectedKeywordsJson) {
        if (expectedKeywordsJson == null || expectedKeywordsJson.isBlank()) {
            return List.of();
        }
        try {
            @SuppressWarnings("unchecked")
            List<String> list = objectMapper.readValue(expectedKeywordsJson, List.class);
            return list != null ? list : List.of();
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    public List<EvalRun> listRuns() {
        return evalRunRepository.findAll();
    }

    public EvalRun getRun(String runKey) {
        return evalRunRepository.findByRunKey(runKey).orElse(null);
    }

    public List<EvalCase> listCases() {
        return evalCaseRepository.findActive();
    }
}
