package com.hrms.common.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Thin service layer around {@link SysUserMapper}.  Higher-level auth orchestration
 * (login, lockout, token issuing) lives in {@code AuthService}.
 */
@Service
@RequiredArgsConstructor
public class SysUserService {

    private final SysUserMapper userMapper;

    public Optional<SysUser> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        SysUser user = userMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, username));
        return Optional.ofNullable(user);
    }

    public Optional<SysUser> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(userMapper.selectById(id));
    }

    public void update(SysUser user) {
        userMapper.updateById(user);
    }
}
