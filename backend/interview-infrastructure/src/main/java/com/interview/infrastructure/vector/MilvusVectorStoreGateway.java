package com.interview.infrastructure.vector;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.application.port.VectorStoreGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class MilvusVectorStoreGateway implements VectorStoreGateway {

    private static final Logger log = LoggerFactory.getLogger(MilvusVectorStoreGateway.class);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String endpoint;
    private final String token;
    private final String collectionName;
    private final int embeddingDim;
    private final boolean enabled;

    private volatile boolean collectionReady;

    public MilvusVectorStoreGateway(
            ObjectMapper objectMapper,
            @Value("${app.milvus.host:127.0.0.1}") String host,
            @Value("${app.milvus.port:19530}") int port,
            @Value("${app.milvus.token:root:Milvus}") String token,
            @Value("${app.milvus.collection:material_chunks_vectors}") String collectionName,
            @Value("${app.milvus.embedding-dim:128}") int embeddingDim,
            @Value("${app.milvus.enabled:true}") boolean enabled
    ) {
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
        this.objectMapper = objectMapper;
        this.endpoint = "http://" + host + ":" + port;
        this.token = token;
        this.collectionName = collectionName;
        this.embeddingDim = Math.max(64, embeddingDim);
        this.enabled = enabled;
    }

    @Override
    public void upsert(List<VectorDocument> documents) {
        if (!enabled || documents == null || documents.isEmpty()) {
            return;
        }

        ensureCollectionReady();

        List<Map<String, Object>> rows = new ArrayList<>(documents.size());
        for (VectorDocument document : documents) {
            if (document == null || document.vector() == null || document.vector().length == 0) {
                continue;
            }
            Map<String, Object> row = new HashMap<>();
            row.put("id", parseNumericId(document.id()));
            row.put("vector", toFloatList(document.vector()));
            row.put("user_id", document.userId());
            row.put("material_id", document.materialId());
            row.put("chunk_no", document.chunkNo());
            row.put("vector_id", document.id());
            row.put("chunk_text", truncate(document.text(), 2000));
            rows.add(row);
        }

        if (rows.isEmpty()) {
            return;
        }

        Map<String, Object> body = new HashMap<>();
        body.put("collectionName", collectionName);
        body.put("data", rows);

        Map<String, Object> response = post("/v2/vectordb/entities/upsert", body);
        assertMilvusSuccess(response, "upsert");
    }

    @Override
    public List<VectorSearchHit> search(float[] queryVector, int topK, Long userId) {
        if (!enabled || queryVector == null || queryVector.length == 0 || userId == null) {
            return List.of();
        }

        ensureCollectionReady();

        Map<String, Object> body = new HashMap<>();
        body.put("collectionName", collectionName);
        body.put("data", List.of(toFloatList(queryVector)));
        body.put("annsField", "vector");
        body.put("limit", Math.max(1, topK));
        body.put("outputFields", List.of("id", "vector_id", "chunk_text", "material_id", "chunk_no"));
        body.put("filter", "user_id == " + userId);

        Map<String, Object> response = post("/v2/vectordb/entities/search", body);
        assertMilvusSuccess(response, "search");
        return parseSearchHits(response);
    }

    private void ensureCollectionReady() {
        if (collectionReady) {
            return;
        }
        synchronized (this) {
            if (collectionReady) {
                return;
            }
            if (!hasCollection()) {
                createCollection();
            }
            loadCollection();
            collectionReady = true;
            log.info("Milvus collection ready: {}", collectionName);
        }
    }

    private boolean hasCollection() {
        Map<String, Object> body = new HashMap<>();
        body.put("collectionName", collectionName);
        Map<String, Object> response = post("/v2/vectordb/collections/has", body);
        assertMilvusSuccess(response, "has collection");

        Object data = response.get("data");
        if (data instanceof Boolean boolValue) {
            return boolValue;
        }
        if (data instanceof Map<?, ?> mapData) {
            Object has = mapData.get("has");
            return has instanceof Boolean bool && bool;
        }
        return false;
    }

    private void createCollection() {
        Map<String, Object> body = new HashMap<>();
        body.put("collectionName", collectionName);
        body.put("dimension", embeddingDim);
        body.put("metricType", "COSINE");
        body.put("autoId", false);
        body.put("enableDynamicField", true);

        Map<String, Object> response = post("/v2/vectordb/collections/create", body);
        assertMilvusSuccess(response, "create collection");
    }

    private void loadCollection() {
        Map<String, Object> body = new HashMap<>();
        body.put("collectionName", collectionName);
        Map<String, Object> response = post("/v2/vectordb/collections/load", body);
        assertMilvusSuccess(response, "load collection");
    }

    private Map<String, Object> post(String path, Map<String, Object> body) {
        try {
            String payload = objectMapper.writeValueAsString(body);
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint + path))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload));

            if (token != null && !token.isBlank()) {
                builder.header("Authorization", "Bearer " + token);
            }

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Milvus HTTP status " + response.statusCode() + " at " + path);
            }
            return objectMapper.readValue(response.body(), new TypeReference<>() {
            });
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("Milvus request failed at " + path + ": " + e.getMessage(), e);
        }
    }

    private void assertMilvusSuccess(Map<String, Object> response, String action) {
        Object code = response.get("code");
        if (code instanceof Number number && number.intValue() == 0) {
            return;
        }
        if (code == null) {
            // Some deployments might not include code on success.
            return;
        }
        Object message = response.getOrDefault("message", response.get("msg"));
        throw new IllegalStateException("Milvus " + action + " failed, code=" + code + ", message=" + message);
    }

    private List<VectorSearchHit> parseSearchHits(Map<String, Object> response) {
        Object data = response.get("data");
        if (!(data instanceof List<?> dataList) || dataList.isEmpty()) {
            return List.of();
        }

        Object first = dataList.get(0);
        if (first instanceof Map<?, ?>) {
            return parseHitMaps(castToMapList(dataList));
        }

        if (first instanceof List<?> nestedList && !nestedList.isEmpty() && nestedList.get(0) instanceof Map<?, ?>) {
            return parseHitMaps(castToMapList((List<?>) first));
        }

        return List.of();
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castToMapList(List<?> raw) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : raw) {
            if (item instanceof Map<?, ?> map) {
                result.add((Map<String, Object>) map);
            }
        }
        return result;
    }

    private List<VectorSearchHit> parseHitMaps(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        List<VectorSearchHit> hits = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String id = Objects.toString(row.get("vector_id"), null);
            if (id == null || id.isBlank()) {
                id = Objects.toString(row.get("id"), null);
            }
            if (id == null || id.isBlank()) {
                continue;
            }
            double score = 0D;
            Object distance = row.get("distance");
            if (distance instanceof Number number) {
                score = number.doubleValue();
            }
            String text = Objects.toString(row.get("chunk_text"), null);
            Long materialId = toLong(row.get("material_id"));
            Integer chunkNo = toInteger(row.get("chunk_no"));
            hits.add(new VectorSearchHit(id, score, text, materialId, chunkNo));
        }
        return hits;
    }

    private Long parseNumericId(String rawId) {
        if (rawId == null || rawId.isBlank()) {
            return System.nanoTime();
        }
        int split = rawId.lastIndexOf('-');
        if (split > 0) {
            try {
                long materialId = Long.parseLong(rawId.substring(0, split));
                int chunkNo = Integer.parseInt(rawId.substring(split + 1));
                return materialId * 1_000_000L + chunkNo;
            } catch (NumberFormatException ignore) {
                // fall through
            }
        }
        try {
            return Long.parseLong(rawId.replaceAll("\\D+", ""));
        } catch (NumberFormatException ex) {
            return Math.abs((long) rawId.hashCode());
        }
    }

    private List<Float> toFloatList(float[] vector) {
        List<Float> values = new ArrayList<>(vector.length);
        for (float value : vector) {
            values.add(value);
        }
        return values;
    }

    private String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() > maxLength ? text.substring(0, maxLength) : text;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
