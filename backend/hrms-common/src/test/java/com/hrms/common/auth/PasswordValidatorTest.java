package com.hrms.common.auth;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidatorTest {

    @Test
    @DisplayName("有效密码通过校验")
    void validPasswordPasses() {
        PasswordValidator.Result result = PasswordValidator.validate("Secure@Pass1", "admin");
        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("密码太短 — 不通过")
    void tooShortFails() {
        PasswordValidator.Result result = PasswordValidator.validate("Ab1", null);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("8"));
    }

    @Test
    @DisplayName("缺少大写字母 — 不通过")
    void missingUpperCaseFails() {
        PasswordValidator.Result result = PasswordValidator.validate("abcdefgh1", null);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("大写"));
    }

    @Test
    @DisplayName("缺少小写字母 — 不通过")
    void missingLowerCaseFails() {
        PasswordValidator.Result result = PasswordValidator.validate("ABCDEFGH1", null);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("小写"));
    }

    @Test
    @DisplayName("缺少数字 — 不通过")
    void missingDigitFails() {
        PasswordValidator.Result result = PasswordValidator.validate("Abcdefghij", null);
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("数字"));
    }

    @Test
    @DisplayName("密码包含用户名（不区分大小写） — 不通过")
    void containsUsernameFails() {
        PasswordValidator.Result result = PasswordValidator.validate("Admin@2026", "admin");
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("用户名"));
    }

    @Test
    @DisplayName("密码不包含用户名 — 通过")
    void doesNotContainUsernamePasses() {
        PasswordValidator.Result result = PasswordValidator.validate("Secure@Pass1", "admin");
        assertThat(result.valid()).isTrue();
        assertThat(result.errors()).isEmpty();
    }

    @Test
    @DisplayName("null 密码 — 不通过")
    void nullPasswordFails() {
        PasswordValidator.Result result = PasswordValidator.validate(null, "admin");
        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(e -> e.contains("不能为空"));
    }

    @Test
    @DisplayName("null 用户名不影响校验")
    void nullUsernameIsIgnored() {
        PasswordValidator.Result result = PasswordValidator.validate("Abcdefg1", null);
        assertThat(result.valid()).isTrue();
    }

    @Test
    @DisplayName("空用户名不影响校验")
    void blankUsernameIsIgnored() {
        PasswordValidator.Result result = PasswordValidator.validate("Abcdefg1", "  ");
        assertThat(result.valid()).isTrue();
    }
}
