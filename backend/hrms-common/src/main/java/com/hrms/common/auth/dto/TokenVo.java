package com.hrms.common.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Setter;

@Data
@Setter
@AllArgsConstructor
public class TokenVo {

    private String accessToken;
    private String refreshToken;
    /** Access-token TTL in seconds. */
    private Long expiresIn;
    private String tokenType;
    private Long uid;
    private Long employeeId;
    private String username;
    /** When {@code true}, the frontend should force the user to change their password. */
    private boolean forceChangePassword;

    public TokenVo(String accessToken, String refreshToken, Long expiresIn, String tokenType,
                   Long uid, Long employeeId, String username) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
        this.tokenType = tokenType;
        this.uid = uid;
        this.employeeId = employeeId;
        this.username = username;
        this.forceChangePassword = false;
    }
}
