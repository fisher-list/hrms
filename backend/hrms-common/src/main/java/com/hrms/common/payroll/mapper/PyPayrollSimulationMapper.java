package com.hrms.common.payroll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.common.payroll.entity.PyPayrollSimulation;
import org.apache.ibatis.annotations.Mapper;

/**
 * 工资模拟运行Mapper接口
 */
@Mapper
public interface PyPayrollSimulationMapper extends BaseMapper<PyPayrollSimulation> {
}
