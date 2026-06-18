package com.hrms.common.auth;

import com.hrms.common.user.SysUser;
import com.hrms.common.user.SysUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Records failed login attempts in a REQUIRES_NEW transaction so the count persists
 * even when the outer login transaction rolls back (BizException is a RuntimeException).
 */
@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    public static final int MAX_FAILED_ATTEMPTS = 5;
    public static final Duration LOCK_DURATION = Duration.ofMinutes(30);

    private final SysUserService userService;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(SysUser user) {
        int attempts = user.getFailedAttempts() == null ? 0 : user.getFailedAttempts();
        attempts += 1;
        user.setFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(LocalDateTime.now(clock).plus(LOCK_DURATION));
        }
        userService.update(user);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void resetFailedAttempts(SysUser user) {
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userService.update(user);
    }
}
