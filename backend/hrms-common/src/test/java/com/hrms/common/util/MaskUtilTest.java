package com.hrms.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MaskUtil} masking operations.
 */
class MaskUtilTest {

    @Test
    @DisplayName("maskIdCard: 18-digit ID card is correctly masked")
    void maskIdCard18Digits() {
        assertThat(MaskUtil.maskIdCard("110101199001011234"))
                .isEqualTo("110***********1234");
    }

    @Test
    @DisplayName("maskIdCard: short input returns as-is")
    void maskIdCardShortInput() {
        assertThat(MaskUtil.maskIdCard("123")).isEqualTo("123");
    }

    @Test
    @DisplayName("maskIdCard: null returns null")
    void maskIdCardNull() {
        assertThat(MaskUtil.maskIdCard(null)).isNull();
    }

    @Test
    @DisplayName("maskPhone: 11-digit phone is correctly masked")
    void maskPhone11Digits() {
        assertThat(MaskUtil.maskPhone("13812345678"))
                .isEqualTo("138****5678");
    }

    @Test
    @DisplayName("maskPhone: short input returns as-is")
    void maskPhoneShortInput() {
        assertThat(MaskUtil.maskPhone("123")).isEqualTo("123");
    }

    @Test
    @DisplayName("maskPhone: null returns null")
    void maskPhoneNull() {
        assertThat(MaskUtil.maskPhone(null)).isNull();
    }

    @Test
    @DisplayName("maskAccountNo: 19-digit account is correctly masked")
    void maskAccountNo19Digits() {
        assertThat(MaskUtil.maskAccountNo("6222021234567890123"))
                .isEqualTo("***************0123");
    }

    @Test
    @DisplayName("maskAccountNo: 4 or fewer chars returns as-is")
    void maskAccountNoShort() {
        assertThat(MaskUtil.maskAccountNo("1234")).isEqualTo("1234");
    }

    @Test
    @DisplayName("maskAccountNo: null returns null")
    void maskAccountNoNull() {
        assertThat(MaskUtil.maskAccountNo(null)).isNull();
    }
}
