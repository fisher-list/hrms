package com.hrms.common.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.common.org.entity.Department;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for department.
 */
@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}
