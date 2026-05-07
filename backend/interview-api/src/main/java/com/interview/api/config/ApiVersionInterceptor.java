package com.interview.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.regex.Pattern;

@Component
public class ApiVersionInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ApiVersionInterceptor.class);
    private static final String SUPPORTED_VERSION = "v1";
    private static final Pattern VERSION_PATH = Pattern.compile("/api/(v\\d+)(?:/|$)");

    private final ObjectMapper objectMapper;

    public ApiVersionInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestUri = request.getRequestURI();
        String version = extractVersion(requestUri);

        if (version != null && !SUPPORTED_VERSION.equals(version)) {
            response.setStatus(400);
            response.setContentType("application/json");
            try {
                objectMapper.writeValue(response.getWriter(),
                        ApiResponse.fail("Unsupported API version: " + version + ". Supported versions: " + SUPPORTED_VERSION));
            } catch (Exception e) {
                log.error("Failed to write error response", e);
            }
            return false;
        }

        response.setHeader("X-API-Version", SUPPORTED_VERSION);
        return true;
    }

    private String extractVersion(String uri) {
        var matcher = VERSION_PATH.matcher(uri);
        return matcher.find() ? matcher.group(1) : null;
    }
}
