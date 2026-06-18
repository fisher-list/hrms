package com.hrms.common.auth;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Password strength validator.
 *
 * <p>Rules:
 * <ul>
 *   <li>Minimum 8 characters</li>
 *   <li>At least one uppercase letter</li>
 *   <li>At least one lowercase letter</li>
 *   <li>At least one digit</li>
 *   <li>Must not contain the username (case-insensitive)</li>
 * </ul>
 */
public final class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPER = Pattern.compile("[A-Z]");
    private static final Pattern LOWER = Pattern.compile("[a-z]");
    private static final Pattern DIGIT = Pattern.compile("[0-9]");

    private PasswordValidator() {
    }

    /**
     * Validate the given password against the strength rules.
     *
     * @param password the password to validate
     * @param username the username that must not appear in the password; may be {@code null}
     * @return a {@link Result} indicating pass/fail and the list of failure reasons
     */
    public static Result validate(String password, String username) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("密码不能为空");
            return new Result(false, errors);
        }

        if (password.length() < MIN_LENGTH) {
            errors.add("密码长度不能少于 " + MIN_LENGTH + " 位");
        }

        if (!UPPER.matcher(password).find()) {
            errors.add("密码必须包含至少一个大写字母");
        }

        if (!LOWER.matcher(password).find()) {
            errors.add("密码必须包含至少一个小写字母");
        }

        if (!DIGIT.matcher(password).find()) {
            errors.add("密码必须包含至少一个数字");
        }

        if (username != null && !username.isBlank()
                && password.toLowerCase().contains(username.toLowerCase())) {
            errors.add("密码不能包含用户名");
        }

        return new Result(errors.isEmpty(), errors);
    }

    /**
     * Validation result.
     */
    public record Result(boolean valid, List<String> errors) {
    }
}
