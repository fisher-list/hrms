package com.hrms.common.exception;

import lombok.Getter;

/**
 * Business-level checked-style runtime exception.
 *
 * <p>Throw to abort a request with a specific {@code code} and message; the
 * {@link com.hrms.common.exception.GlobalExceptionHandler} will serialize it into
 * a unified {@code R} envelope without logging a stacktrace.</p>
 */
@Getter
public class BizException extends RuntimeException {

    private final int code;

    public BizException(int code, String msg) {
        super(msg);
        this.code = code;
    }
}
