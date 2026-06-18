package com.hrms.common.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.common.employee.entity.HrProbationConversion;
import org.apache.ibatis.annotations.Mapper;

/**
 * 试用期转正申请单Mapper。
 */
@Mapper
public interface HrProbationConversionMapper extends BaseMapper<HrProbationConversion> {
}
