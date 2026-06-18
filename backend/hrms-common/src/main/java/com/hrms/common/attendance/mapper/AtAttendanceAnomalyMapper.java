package com.hrms.common.attendance.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.common.attendance.entity.AtAttendanceAnomaly;
import org.apache.ibatis.annotations.Mapper;

/**
 * 考勤异常记录 Mapper
 */
@Mapper
public interface AtAttendanceAnomalyMapper extends BaseMapper<AtAttendanceAnomaly> {
}
