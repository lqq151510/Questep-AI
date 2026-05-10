package com.interview.application.service;

import com.interview.application.dto.CaptchaResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Service
public class CaptchaService {
    private static final Logger logger = LoggerFactory.getLogger(CaptchaService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int CODE_LENGTH = 4;

    private final Map<String, CaptchaEntry> store = new ConcurrentHashMap<>();
    private final long ttlSeconds;

    public CaptchaService(@Value("${app.captcha.ttl-seconds:300}") long ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
        Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "captcha-cleaner");
            t.setDaemon(true);
            return t;
        }).scheduleAtFixedRate(this::evictExpired, ttlSeconds, ttlSeconds, TimeUnit.SECONDS);
    }

    public CaptchaResponse generate() {
        String captchaId = UUID.randomUUID().toString();
        String code = String.format("%0" + CODE_LENGTH + "d", RANDOM.nextInt((int) Math.pow(10, CODE_LENGTH)));
        store.put(captchaId, new CaptchaEntry(code, System.currentTimeMillis() + ttlSeconds * 1000));
        logger.debug("Captcha generated: id={} code={}", captchaId, code);
        return new CaptchaResponse(captchaId, code);
    }

    public boolean validate(String captchaId, String captchaCode) {
        if (captchaId == null || captchaCode == null) {
            return false;
        }
        CaptchaEntry entry = store.remove(captchaId);
        if (entry == null) {
            return false;
        }
        if (System.currentTimeMillis() > entry.expireAt) {
            return false;
        }
        return entry.code.equals(captchaCode.trim());
    }

    private void evictExpired() {
        long now = System.currentTimeMillis();
        store.entrySet().removeIf(e -> now > e.getValue().expireAt);
    }

    private record CaptchaEntry(String code, long expireAt) {}
}
