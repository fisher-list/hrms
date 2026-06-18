package com.hrms.common.rbac.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.common.rbac.entity.SysUserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Set;

/**
 * MyBatis-Plus mapper for sys_user_role.
 */
@Mapper
public interface SysUserRoleMapper extends BaseMapper<SysUserRole> {

    /**
     * Single JOIN query to load all enabled permission codes for a user.
     * Replaces 4 separate queries in PermissionService.loadPermissionCodes().
     */
    @Select("""
            SELECT DISTINCT p.code
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id AND r.enabled = true
            JOIN sys_role_permission rp ON rp.role_id = r.id
            JOIN sys_permission p ON p.id = rp.permission_id
            WHERE ur.user_id = #{userId}
            """)
    Set<String> selectPermissionCodesByUserId(@Param("userId") Long userId);

    /**
     * ADMIN is a built-in super role. Checking the role code makes this robust even
     * when legacy seed data mapped later module permissions to the wrong role id.
     */
    @Select("""
            SELECT COUNT(1) > 0
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id AND r.enabled = true
            WHERE ur.user_id = #{userId}
              AND r.code = 'ADMIN'
            """)
    boolean userHasAdminRole(@Param("userId") Long userId);

    /**
     * Return all distinct data_scope values from enabled roles for a user.
     * Used by DataScopeAspect to determine the dominant data scope.
     */
    @Select("""
            SELECT DISTINCT r.data_scope
            FROM sys_user_role ur
            JOIN sys_role r ON r.id = ur.role_id AND r.enabled = true
            WHERE ur.user_id = #{userId}
            """)
    Set<String> selectDataScopesByUserId(@Param("userId") Long userId);
}
