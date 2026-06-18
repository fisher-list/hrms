package com.hrms.common.util;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * AES-256-GCM encryption utility for sensitive fields (id_card, phone, account_no).
 *
 * <p>Key is read from {@code HRMS_AES_KEY} env var, which MUST be set and exactly
 * 32 bytes (UTF-8). The application will fail to start if the variable is missing
 * or has the wrong length. Generate a valid key with:
 * {@code openssl rand -base64 32}</p>
 *
 * <p>Ciphertext format: 12-byte IV || AES-GCM ciphertext || 16-byte GCM tag,
 * all base64-encoded.</p>
 */
public final class AesUtil {

    private static final String ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int KEY_LENGTH_BYTES = 32;

    private static final SecretKeySpec KEY;

    static {
        String envKey = System.getenv("HRMS_AES_KEY");
        // Fallback to system property for testing (set via -DHRMS_AES_KEY=...)
        if (envKey == null || envKey.isBlank()) {
            envKey = System.getProperty("HRMS_AES_KEY");
        }
        if (envKey == null || envKey.isBlank()) {
            throw new IllegalStateException(
                    "AES key is not configured. Set the HRMS_AES_KEY environment variable (must be exactly 32 bytes). "
                    + "Generate one with: openssl rand -base64 32");
        }
        byte[] keyBytes = envKey.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "HRMS_AES_KEY must be exactly 32 bytes, but was " + keyBytes.length + " bytes. "
                    + "Generate a valid key with: openssl rand -base64 32");
        }
        KEY = new SecretKeySpec(keyBytes, "AES");
    }

    private AesUtil() {
    }

    /**
     * Encrypt plaintext with AES-256-GCM.
     *
     * @param plaintext the text to encrypt
     * @return base64-encoded ciphertext (IV prepended)
     */
    public static String encrypt(String plaintext) {
        if (plaintext == null) {
            return null;
        }
        try {
            byte[] iv = new byte[IV_LENGTH_BYTES];
            new SecureRandom().nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.ENCRYPT_MODE, KEY, spec);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // prepend IV to ciphertext
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv);
            buffer.put(encrypted);

            return Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM encryption failed", e);
        }
    }

    /**
     * Decrypt base64-encoded AES-256-GCM ciphertext.
     *
     * @param base64Ciphertext base64-encoded ciphertext with IV prepended
     * @return decrypted plaintext
     */
    public static String decrypt(String base64Ciphertext) {
        if (base64Ciphertext == null) {
            return null;
        }
        try {
            byte[] decoded = Base64.getDecoder().decode(base64Ciphertext);
            ByteBuffer buffer = ByteBuffer.wrap(decoded);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            buffer.get(iv);

            byte[] encrypted = new byte[buffer.remaining()];
            buffer.get(encrypted);

            Cipher cipher = Cipher.getInstance(ALGORITHM);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv);
            cipher.init(Cipher.DECRYPT_MODE, KEY, spec);
            byte[] decrypted = cipher.doFinal(encrypted);

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("AES-GCM decryption failed", e);
        }
    }
}
