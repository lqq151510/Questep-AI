package com.interview.application.service;

import com.interview.application.port.VectorStoreGateway;
import com.interview.domain.model.MaterialChunk;
import com.interview.domain.model.MaterialChunkDraft;
import com.interview.domain.repository.MaterialChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class MaterialRagApplicationService {
    private static final Logger log = LoggerFactory.getLogger(MaterialRagApplicationService.class);
    private static final Pattern TOKEN_SPLIT = Pattern.compile("[^\\p{L}\\p{N}]+", Pattern.UNICODE_CHARACTER_CLASS);
    private static final String EMBEDDING_MODEL_NAME = "hashing-v1";
    private static final int DEFAULT_FALLBACK_FETCH_SIZE = 80;

    private final MaterialChunkRepository materialChunkRepository;
    private final VectorStoreGateway vectorStoreGateway;
    private final boolean milvusEnabled;
    private final int embeddingDim;
    private final int chunkSize;
    private final int chunkOverlap;

    public MaterialRagApplicationService(
            MaterialChunkRepository materialChunkRepository,
            VectorStoreGateway vectorStoreGateway,
            @Value("${app.milvus.enabled:true}") boolean milvusEnabled,
            @Value("${app.milvus.embedding-dim:128}") int embeddingDim,
            @Value("${app.milvus.chunk-size:500}") int chunkSize,
            @Value("${app.milvus.chunk-overlap:80}") int chunkOverlap
    ) {
        this.materialChunkRepository = materialChunkRepository;
        this.vectorStoreGateway = vectorStoreGateway;
        this.milvusEnabled = milvusEnabled;
        this.embeddingDim = clamp(embeddingDim, 64, 2048);
        this.chunkSize = clamp(chunkSize, 128, 4000);
        this.chunkOverlap = clamp(chunkOverlap, 0, this.chunkSize / 2);
    }

    public void indexMaterial(Long userId, Long materialId, String content) {
        if (userId == null || materialId == null || content == null || content.isBlank()) {
            return;
        }

        List<String> chunks = splitIntoChunks(content);
        if (chunks.isEmpty()) {
            return;
        }

        List<MaterialChunkDraft> drafts = new ArrayList<>(chunks.size());
        List<VectorStoreGateway.VectorDocument> docs = new ArrayList<>(chunks.size());
        for (int i = 0; i < chunks.size(); i++) {
            String chunkText = chunks.get(i);
            String vectorId = buildVectorId(materialId, i);
            float[] vector = embed(chunkText);

            drafts.add(new MaterialChunkDraft(
                    i,
                    chunkText,
                    approxTokenCount(chunkText),
                    EMBEDDING_MODEL_NAME,
                    vectorId,
                    "{\"materialId\":" + materialId + ",\"chunkNo\":" + i + "}"
            ));
            docs.add(new VectorStoreGateway.VectorDocument(
                    vectorId,
                    userId,
                    materialId,
                    i,
                    chunkText,
                    vector
            ));
        }

        materialChunkRepository.replaceChunks(materialId, drafts);

        if (!milvusEnabled) {
            return;
        }
        try {
            vectorStoreGateway.upsert(docs);
        } catch (Exception ex) {
            log.warn("Milvus upsert failed, fallback to local chunk store only. materialId={}, error={}", materialId, ex.getMessage());
        }
    }

    public List<String> retrieveContext(Long userId, String query, int topK) {
        if (userId == null || query == null || query.isBlank()) {
            return List.of();
        }
        int safeTopK = clamp(topK, 1, 8);
        float[] queryVector = embed(query);

        if (milvusEnabled) {
            try {
                List<VectorStoreGateway.VectorSearchHit> hits = vectorStoreGateway.search(queryVector, safeTopK, userId);
                List<String> hitTexts = hits.stream()
                        .map(VectorStoreGateway.VectorSearchHit::text)
                        .filter(text -> text != null && !text.isBlank())
                        .toList();
                if (!hitTexts.isEmpty()) {
                    return deduplicateKeepOrder(hitTexts, safeTopK);
                }

                List<String> byIdTexts = resolveHitsFromLocalChunks(hits, safeTopK);
                if (!byIdTexts.isEmpty()) {
                    return byIdTexts;
                }
            } catch (Exception ex) {
                log.warn("Milvus search failed, using local keyword fallback. userId={}, error={}", userId, ex.getMessage());
            }
        }

        return fallbackKeywordSearch(userId, query, safeTopK);
    }

    private List<String> resolveHitsFromLocalChunks(List<VectorStoreGateway.VectorSearchHit> hits, int topK) {
        if (hits == null || hits.isEmpty()) {
            return List.of();
        }
        List<String> ids = hits.stream().map(VectorStoreGateway.VectorSearchHit::id).toList();
        List<MaterialChunk> chunks = materialChunkRepository.findByVectorIds(ids);
        if (chunks.isEmpty()) {
            return List.of();
        }
        Map<String, String> chunkByVectorId = new HashMap<>();
        for (MaterialChunk chunk : chunks) {
            if (chunk.vectorId() != null && chunk.chunkText() != null && !chunk.chunkText().isBlank()) {
                chunkByVectorId.put(chunk.vectorId(), chunk.chunkText());
            }
        }

        List<String> ordered = new ArrayList<>();
        for (String id : ids) {
            String text = chunkByVectorId.get(id);
            if (text != null && !text.isBlank()) {
                ordered.add(text);
            }
            if (ordered.size() >= topK) {
                break;
            }
        }
        return deduplicateKeepOrder(ordered, topK);
    }

    private List<String> fallbackKeywordSearch(Long userId, String query, int topK) {
        List<MaterialChunk> candidates = materialChunkRepository.findRecentByUserId(userId, DEFAULT_FALLBACK_FETCH_SIZE);
        if (candidates.isEmpty()) {
            return List.of();
        }

        Set<String> queryTokens = tokenize(query);
        List<ScoredChunk> scored = new ArrayList<>();
        for (MaterialChunk chunk : candidates) {
            String text = chunk.chunkText();
            if (text == null || text.isBlank()) {
                continue;
            }
            double score = keywordOverlapScore(queryTokens, tokenize(text));
            if (score > 0) {
                scored.add(new ScoredChunk(text, score));
            }
        }

        scored.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
        List<String> result = new ArrayList<>();
        for (ScoredChunk item : scored) {
            result.add(item.text());
            if (result.size() >= topK) {
                break;
            }
        }
        return deduplicateKeepOrder(result, topK);
    }

    private double keywordOverlapScore(Set<String> queryTokens, Set<String> textTokens) {
        if (queryTokens.isEmpty() || textTokens.isEmpty()) {
            return 0D;
        }
        int hitCount = 0;
        for (String token : queryTokens) {
            if (textTokens.contains(token)) {
                hitCount++;
            }
        }
        return (double) hitCount / (double) queryTokens.size();
    }

    private List<String> splitIntoChunks(String content) {
        String normalized = content.strip();
        if (normalized.isBlank()) {
            return List.of();
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        int length = normalized.length();
        while (start < length) {
            int end = Math.min(length, start + chunkSize);
            String chunk = normalized.substring(start, end).strip();
            if (!chunk.isBlank()) {
                chunks.add(chunk);
            }
            if (end >= length) {
                break;
            }
            start = Math.max(end - chunkOverlap, start + 1);
        }
        return chunks;
    }

    private float[] embed(String text) {
        float[] vector = new float[embeddingDim];
        Set<String> tokens = tokenize(text);
        if (tokens.isEmpty()) {
            return vector;
        }
        for (String token : tokens) {
            int hash = token.hashCode();
            int index = Math.floorMod(hash, embeddingDim);
            float delta = ((hash >>> 1) & 1) == 0 ? 1.0f : -1.0f;
            vector[index] += delta;
        }
        normalizeL2(vector);
        return vector;
    }

    private Set<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        String[] parts = TOKEN_SPLIT.split(text.toLowerCase(Locale.ROOT));
        Set<String> tokens = new LinkedHashSet<>();
        for (String part : parts) {
            String token = part.strip();
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }
        if (tokens.isEmpty() && text.length() >= 2) {
            // Fallback for CJK text without delimiters.
            for (int i = 0; i < text.length() - 1; i++) {
                String biGram = text.substring(i, i + 2).strip();
                if (!biGram.isBlank()) {
                    tokens.add(biGram);
                }
            }
        }
        return tokens;
    }

    private void normalizeL2(float[] vector) {
        double norm = 0D;
        for (float value : vector) {
            norm += value * value;
        }
        if (norm <= 0D) {
            return;
        }
        double divisor = Math.sqrt(norm);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = (float) (vector[i] / divisor);
        }
    }

    private int approxTokenCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, text.length() / 2);
    }

    private String buildVectorId(Long materialId, int chunkNo) {
        return materialId + "-" + chunkNo;
    }

    private List<String> deduplicateKeepOrder(List<String> values, int limit) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                set.add(value);
            }
            if (set.size() >= limit) {
                break;
            }
        }
        return new ArrayList<>(set);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record ScoredChunk(String text, double score) {
    }
}
