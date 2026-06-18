package com.hrms.common.util;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

/**
 * Deterministic keyed hashes for equality checks on encrypted sensitive fields.
 */
public final class SensitiveHashUtil {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final byte[] KEY_BYTES;

    static {
        String envKey = System.getenv("HRMS_AES_KEY");
        // Fallback to system property for testing (set via -DHRMS_AES_KEY=...)
        if (envKey == null || envKey.isBlank()) {
            envKey = System.getProperty("HRMS_AES_KEY");
        }
        if (envKey == null || envKey.isBlank()) {
            throw new IllegalStateException(
                    "Sensitive hash key is not configured. Set HRMS_AES_KEY (must be exactly 32 bytes).");
        }
        KEY_BYTES = envKey.getBytes(StandardCharsets.UTF_8);
        if (KEY_BYTES.length != KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "HRMS_AES_KEY must be exactly 32 bytes, but was " + KEY_BYTES.length + " bytes.");
        }
    }

    private SensitiveHashUtil() {
    }

    public static String idCardHash(String idCard) {
        String normalized = normalizeIdCard(idCard);
        if (normalized == null) {
            return null;
        }
        return hmacHex(normalized);
    }

    private static String normalizeIdCard(String idCard) {
        if (idCard == null) {
            return null;
        }
        String normalized = idCard.trim().toUpperCase();
        return normalized.isEmpty() ? null : normalized;
    }

    private static String hmacHex(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(KEY_BYTES, HMAC_ALGORITHM));
            byte[] digest = mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Sensitive field hashing failed", e);
        }
    }
}
