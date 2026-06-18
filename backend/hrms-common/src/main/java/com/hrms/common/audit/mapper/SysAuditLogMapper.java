package com.hrms.common.audit.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.common.audit.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
}
