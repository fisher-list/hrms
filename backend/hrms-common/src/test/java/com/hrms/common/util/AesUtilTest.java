package com.hrms.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AesUtil} encrypt/decrypt operations.
 */
class AesUtilTest {

    @Test
    @DisplayName("encrypt/decrypt roundtrip preserves original plaintext")
    void encryptDecryptRoundtrip() {
        String plaintext = "110101199001011234";
        String encrypted = AesUtil.encrypt(plaintext);

        assertThat(encrypted).isNotNull().isNotEqualTo(plaintext);
        assertThat(AesUtil.decrypt(encrypted)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("encrypt/decrypt roundtrip for phone number")
    void encryptDecryptRoundtripPhone() {
        String phone = "13812345678";
        String encrypted = AesUtil.encrypt(phone);

        assertThat(encrypted).isNotNull().isNotEqualTo(phone);
        assertThat(AesUtil.decrypt(encrypted)).isEqualTo(phone);
    }

    @Test
    @DisplayName("same plaintext produces different ciphertext (random IV)")
    void samePlaintextProducesDifferentCiphertext() {
        String plaintext = "test-data-123";
        String cipher1 = AesUtil.encrypt(plaintext);
        String cipher2 = AesUtil.encrypt(plaintext);

        // With random IV, the two ciphertexts must differ
        assertThat(cipher1).isNotEqualTo(cipher2);

        // Both must decrypt to the same plaintext
        assertThat(AesUtil.decrypt(cipher1)).isEqualTo(plaintext);
        assertThat(AesUtil.decrypt(cipher2)).isEqualTo(plaintext);
    }

    @Test
    @DisplayName("encrypt returns null for null input")
    void encryptNullReturnsNull() {
        assertThat(AesUtil.encrypt(null)).isNull();
    }

    @Test
    @DisplayName("decrypt returns null for null input")
    void decryptNullReturnsNull() {
        assertThat(AesUtil.decrypt(null)).isNull();
    }
}
