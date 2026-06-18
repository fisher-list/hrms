package com.hrms.common.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Set;

@Data
@AllArgsConstructor
public class CurrentUserVo {

    private Long uid;
    private Long employeeId;
    private String username;
    private String nickname;
    private Set<String> roles;
    /** When {@code true}, the frontend should force the user to change their password. */
    private boolean forceChangePassword;

    public CurrentUserVo(Long uid, Long employeeId, String username, String nickname, Set<String> roles) {
        this(uid, employeeId, username, nickname, roles, false);
    }
}
