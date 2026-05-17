package com.interview.application.service.quiz;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
public class WebSearchService {
    private static final Logger logger = LoggerFactory.getLogger(WebSearchService.class);
    private static final int MAX_CONTEXT_LENGTH = 2000;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public WebSearchService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
    }

    public String searchContext(String query) {
        if (query == null || query.isBlank()) {
            return "";
        }
        try {
            String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
            URI uri = URI.create("https://api.duckduckgo.com/?q=" + encoded + "&format=json&no_html=1&skip_disambig=1");
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .GET()
                    .timeout(Duration.ofSeconds(5))
                    .header("Accept", "application/json")
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "";
            }
            return toContextText(response.body());
        } catch (Exception ex) {
            logger.warn("Web search failed for query='{}': {}", query, ex.getMessage());
            return "";
        }
    }

    private String toContextText(String body) throws IOException {
        JsonNode root = objectMapper.readTree(body);
        List<String> lines = new ArrayList<>();
        String abstractText = root.path("AbstractText").asText("");
        if (!abstractText.isBlank()) {
            lines.add("摘要: " + abstractText.trim());
        }

        JsonNode related = root.path("RelatedTopics");
        if (related.isArray()) {
            int added = 0;
            for (JsonNode item : related) {
                if (added >= 4) {
                    break;
                }
                String text = item.path("Text").asText("");
                if (!text.isBlank()) {
                    lines.add("要点" + (added + 1) + ": " + text.trim());
                    added++;
                }
            }
        }

        String merged = String.join("\n", lines);
        if (merged.length() > MAX_CONTEXT_LENGTH) {
            return merged.substring(0, MAX_CONTEXT_LENGTH);
        }
        return merged;
    }
}
