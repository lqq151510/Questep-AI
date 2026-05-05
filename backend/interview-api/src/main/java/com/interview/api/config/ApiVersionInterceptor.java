package com.interview.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiVersionInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(ApiVersionInterceptor.class);
    private static final String SUPPORTED_VERSION = "v1";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String requestUri = request.getRequestURI();
        String version = extractVersion(requestUri);
        
        if (version != null && !SUPPORTED_VERSION.equals(version)) {
            response.setStatus(400);
            response.setContentType("application/json");
            try {
                response.getWriter().write(
                    "{\"success\":false,\"message\":\"Unsupported API version: " + version + 
                    ". Supported versions: " + SUPPORTED_VERSION + "\"}"
                );
            } catch (Exception e) {
                log.error("Failed to write error response", e);
            }
            return false;
        }
        
        response.setHeader("X-API-Version", SUPPORTED_VERSION);
        return true;
    }

    private String extractVersion(String uri) {
        String[] parts = uri.split("/");
        for (int i = 0; i < parts.length; i++) {
            if ("api".equals(parts[i]) && i + 1 < parts.length) {
                return parts[i + 1];
            }
        }
        return null;
    }
}
