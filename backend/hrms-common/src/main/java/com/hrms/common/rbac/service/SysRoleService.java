package com.hrms.common.rbac.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;
import com.hrms.common.rbac.dto.RoleDto;
import com.hrms.common.rbac.entity.SysRole;
import com.hrms.common.rbac.mapper.SysRoleMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for RBAC role CRUD.
 */
@Service
@RequiredArgsConstructor
public class SysRoleService {

    private final SysRoleMapper roleMapper;
    private final PermissionService permissionService;

    public List<SysRole> listAll() {
        return roleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().orderByAsc(SysRole::getCode));
    }

    public SysRole getById(Long id) {
        SysRole role = roleMapper.selectById(id);
        if (role == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Role not found: " + id);
        }
        return role;
    }

    @Transactional
    public SysRole create(RoleDto dto) {
        // check unique code
        Long existing = roleMapper.selectCount(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getCode, dto.getCode()));
        if (existing > 0) {
            throw new BizException(BizCode.BAD_REQUEST, "Role code already exists: " + dto.getCode());
        }
        SysRole role = new SysRole();
        role.setCode(dto.getCode());
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        role.setBuiltin(false);
        role.setEnabled(dto.getEnabled() != null ? dto.getEnabled() : true);
        role.setDataScope(dto.getDataScope() != null ? dto.getDataScope() : "SELF_ONLY");
        roleMapper.insert(role);
        return role;
    }

    @Transactional
    public SysRole update(Long id, RoleDto dto) {
        SysRole role = getById(id);
        if (Boolean.TRUE.equals(role.getBuiltin())) {
            throw new BizException(BizCode.BAD_REQUEST, "Cannot modify builtin role");
        }
        role.setCode(dto.getCode());
        role.setName(dto.getName());
        role.setDescription(dto.getDescription());
        if (dto.getEnabled() != null) {
            role.setEnabled(dto.getEnabled());
        }
        if (dto.getDataScope() != null) {
            role.setDataScope(dto.getDataScope());
        }
        roleMapper.updateById(role);
        // evict all permission caches since role data changed
        permissionService.evictAllCache();
        return role;
    }

    @Transactional
    public void delete(Long id) {
        SysRole role = getById(id);
        if (Boolean.TRUE.equals(role.getBuiltin())) {
            throw new BizException(BizCode.BAD_REQUEST, "Cannot delete builtin role");
        }
        roleMapper.deleteById(id);
        permissionService.evictAllCache();
    }
}
