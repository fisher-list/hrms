package com.hrms.common.user;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * Refresh-token registry / blacklist (see data-model §7.6.1).
 *
 * <p>The actual refresh token JWT is hashed (SHA-256) before storage; we never persist
 * raw tokens.  The {@code refreshJti} is also indexed unique so a stolen refresh token
 * fingerprint can be revoked individually.</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_token")
public class SysUserToken extends BaseEntity {

    private Long userId;

    private String refreshJti;

    private String refreshTokenHash;

    private String device;

    private String ip;

    private LocalDateTime issuedAt;

    private LocalDateTime expiresAt;

    private Boolean revoked;

    private LocalDateTime revokedAt;
}
