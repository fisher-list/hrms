package com.hrms.app.logging;

import ch.qos.logback.classic.pattern.ThrowableProxyConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.CoreConstants;
import ch.qos.logback.core.LayoutBase;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Lightweight structured JSON log layout for Logback.
 * <p>
 * Produces one JSON object per line (NDJSON) without requiring any
 * external dependency such as logstash-logback-encoder.
 * <p>
 * Output example:
 * <pre>
 * {"timestamp":"2026-06-18T10:30:00.123+08:00","level":"INFO","logger":"c.h.s.UserService","thread":"http-nio-8080-exec-1","message":"User logged in","traceId":"abc123"}
 * </pre>
 */
public class JsonLayout extends LayoutBase<ILoggingEvent> {

    private static final DateTimeFormatter ISO_FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
                    .withZone(ZoneId.systemDefault());

    private String appenderName;

    /* ---- configurable properties (set via logback <layout> configuration) ---- */

    public void setAppenderName(String appenderName) {
        this.appenderName = appenderName;
    }

    /* ---- LayoutBase contract ---- */

    @Override
    public String doLayout(ILoggingEvent event) {
        if (!isStarted()) {
            return CoreConstants.EMPTY_STRING;
        }

        Map<String, Object> map = new LinkedHashMap<>(16);

        // timestamp
        map.put("timestamp", ISO_FMT.format(Instant.ofEpochMilli(event.getTimeStamp())));

        // level
        map.put("level", event.getLevel().toString());

        // logger (abbreviated)
        map.put("logger", abbreviate(event.getLoggerName()));

        // thread
        map.put("thread", event.getThreadName());

        // message
        map.put("message", event.getFormattedMessage());

        // MDC fields (traceId, spanId, userId, etc.)
        Map<String, String> mdc = event.getMDCPropertyMap();
        if (mdc != null && !mdc.isEmpty()) {
            map.put("mdc", mdc);
        }

        // exception
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp != null) {
            ThrowableProxyConverter converter = new ThrowableProxyConverter();
            converter.start();
            String stackTrace = converter.convert(event);
            converter.stop();
            map.put("exception", stackTrace);
        }

        // app name (optional, useful in aggregated logging)
        if (appenderName != null) {
            map.put("app", appenderName);
        }

        return toJson(map) + CoreConstants.LINE_SEPARATOR;
    }

    /* ---- helpers ---- */

    /**
     * Abbreviate logger name: com.hrms.service.UserService -> c.h.s.UserService
     */
    private String abbreviate(String loggerName) {
        if (loggerName == null) {
            return "";
        }
        String[] parts = loggerName.split("\\.");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i == parts.length - 1) {
                if (sb.length() > 0) sb.append('.');
                sb.append(parts[i]);
            } else {
                sb.append(parts[i].charAt(0)).append('.');
            }
        }
        return sb.toString();
    }

    /**
     * Minimal JSON serializer — avoids jackson dependency inside the layout.
     * Handles String, Map, and null values.
     */
    private String toJson(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> e : map.entrySet()) {
            if (!first) sb.append(',');
            first = false;
            appendJsonString(sb, e.getKey());
            sb.append(':');
            Object v = e.getValue();
            if (v == null) {
                sb.append("null");
            } else if (v instanceof Map) {
                sb.append(toJson((Map<String, Object>) v));
            } else {
                appendJsonString(sb, v.toString());
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private void appendJsonString(StringBuilder sb, String value) {
        sb.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"':  sb.append("\\\""); break;
                case '\\': sb.append("\\\\"); break;
                case '\n': sb.append("\\n");  break;
                case '\r': sb.append("\\r");  break;
                case '\t': sb.append("\\t");  break;
                default:
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
    }
}
