package com.hrms.common.api;

import lombok.Getter;

/**
 * Unified API response envelope.
 *
 * <p>HTTP status is always 200 for business outcomes; the {@code code} field carries the
 * business semantic and 401/403 are also wrapped in this envelope.</p>
 */
@Getter
public class R<T> {

    /** Business code, see {@link BizCode}. */
    private final int code;

    /** Human readable message. */
    private final String msg;

    /** Payload, may be {@code null}. */
    private final T data;

    private R(int code, String msg, T data) {
        this.code = code;
        this.msg = msg;
        this.data = data;
    }

    public static <T> R<T> ok() {
        return new R<>(BizCode.OK, "ok", null);
    }

    public static <T> R<T> ok(T data) {
        return new R<>(BizCode.OK, "ok", data);
    }

    public static <T> R<T> fail(int code, String msg) {
        return new R<>(code, msg, null);
    }
}
