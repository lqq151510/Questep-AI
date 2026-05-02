package com.interview.api.security;

import com.interview.application.service.TokenBlacklistService;
import com.interview.application.service.TokenService;
import com.interview.domain.model.User;
import com.interview.domain.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final TokenService tokenService;
    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

    public JwtAuthenticationFilter(
            TokenService tokenService,
            UserRepository userRepository,
            TokenBlacklistService tokenBlacklistService
    ) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            String token = auth.substring(7).trim();
            if (!tokenBlacklistService.isBlacklisted(token)) {
                authenticate(token, request);
            }
        }
        chain.doFilter(request, response);
    }

    private void authenticate(String token, HttpServletRequest request) {
        if (!StringUtils.hasText(token)) {
            return;
        }
        try {
            Long uid = tokenService.parseUserId(token);
            User user = userRepository.findById(uid).orElse(null);
            if (user != null && user.status() != null && user.status() == 1) {
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(
                                user.id(),
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_USER"))
                        )
                );
            }
        } catch (ExpiredJwtException ex) {
            log.warn("JWT expired for {}: {}", request.getRequestURI(), ex.getMessage());
        } catch (JwtException | IllegalArgumentException ex) {
            log.warn("Invalid JWT for {}: {}", request.getRequestURI(), ex.getMessage());
        }
    }
}
