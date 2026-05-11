package com.interview.api.websocket;

import com.interview.application.service.TokenBlacklistService;
import com.interview.application.service.TokenService;
import com.interview.domain.model.User;
import com.interview.domain.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
public class JwtWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "ws_user_id";

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtWebSocketHandshakeInterceptor(
            TokenService tokenService,
            UserRepository userRepository,
            TokenBlacklistService tokenBlacklistService
    ) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes
    ) {
        String token = resolveToken(request);
        if (!StringUtils.hasText(token) || tokenBlacklistService.isBlacklisted(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }

        try {
            Long userId = tokenService.parseUserId(token);
            User user = userRepository.findById(userId).orElse(null);
            if (user == null || user.status() == null || user.status() != User.STATUS_ACTIVE) {
                response.setStatusCode(HttpStatus.UNAUTHORIZED);
                return false;
            }
            attributes.put(ATTR_USER_ID, user.id());
            return true;
        } catch (Exception ex) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception
    ) {
        // no-op
    }

    private String resolveToken(ServerHttpRequest request) {
        String auth = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            return auth.substring(7).trim();
        }
        String rawQuery = request.getURI().getRawQuery();
        if (!StringUtils.hasText(rawQuery)) {
            return null;
        }

        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = pair.substring(0, idx);
            if (!"token".equals(key)) {
                continue;
            }
            return URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
        }
        return null;
    }
}
