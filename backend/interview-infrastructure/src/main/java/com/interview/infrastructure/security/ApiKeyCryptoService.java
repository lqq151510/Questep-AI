package com.interview.infrastructure.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class ApiKeyCryptoService {
    private static final String ENCRYPTION_PREFIX = "enc:v1:";
    private static final int IV_LENGTH = 12;

    private final SecretKeySpec secretKeySpec;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyCryptoService(
            @Value("${app.llm.settings-encryption-key:${JWT_SECRET:interview-default-llm-settings-key}}")
            String keyMaterial
    ) {
        this.secretKeySpec = new SecretKeySpec(sha256Bytes(keyMaterial), "AES");
    }

    public String encryptForStorage(String plainText) {
        String normalized = trimToNull(plainText);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith(ENCRYPTION_PREFIX)) {
            return normalized;
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(normalized.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return ENCRYPTION_PREFIX + Base64.getEncoder().encodeToString(payload);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encrypt user LLM api key", ex);
        }
    }

    public String decryptFromStorage(String storedValue) {
        String normalized = trimToNull(storedValue);
        if (normalized == null) {
            return null;
        }
        if (!normalized.startsWith(ENCRYPTION_PREFIX)) {
            return normalized;
        }
        try {
            byte[] payload = Base64.getDecoder().decode(normalized.substring(ENCRYPTION_PREFIX.length()));
            if (payload.length <= IV_LENGTH) {
                throw new IllegalStateException("Encrypted api key payload is invalid");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_LENGTH);
            byte[] ciphertext = Arrays.copyOfRange(payload, IV_LENGTH, payload.length);

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new GCMParameterSpec(128, iv));
            return trimToNull(new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decrypt user LLM api key", ex);
        }
    }

    private byte[] sha256Bytes(String value) {
        String normalized = trimToNull(value);
        String effective = normalized == null ? "interview-default-llm-settings-key" : normalized;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(effective.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize api key encryption key", ex);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }
}
