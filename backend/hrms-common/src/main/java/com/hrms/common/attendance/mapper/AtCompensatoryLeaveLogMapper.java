package com.hrms.common.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.common.attendance.entity.AtCompensatoryLeaveLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 调休余额变动日志 Mapper
 */
@Mapper
public interface AtCompensatoryLeaveLogMapper extends BaseMapper<AtCompensatoryLeaveLog> {
}
