package com.hrms.common.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.user.SysUserToken;
import com.hrms.common.user.SysUserTokenMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Periodic cleanup of expired and revoked refresh-token records from
 * {@code sys_user_token}.  Keeps the table from growing unboundedly.
 *
 * <p>Schedule: every day at 03:00 (server-local time).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenCleanupService {

    private final SysUserTokenMapper tokenMapper;

    /**
     * Delete token records that are both expired <em>and</em> revoked.
     * Revoked-only records are kept until they expire so that refresh-token
     * rotation auditing remains possible within the TTL window.
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanupExpiredRevokedTokens() {
        LocalDateTime now = LocalDateTime.now();
        int deleted = tokenMapper.delete(
                new LambdaQueryWrapper<SysUserToken>()
                        .eq(SysUserToken::getRevoked, true)
                        .lt(SysUserToken::getExpiresAt, now)
        );
        log.info("Token cleanup: removed {} expired & revoked records", deleted);
    }
}
