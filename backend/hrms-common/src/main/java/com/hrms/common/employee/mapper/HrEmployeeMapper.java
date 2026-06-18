package com.hrms.common.employee.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.hrms.common.employee.entity.HrEmployee;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface HrEmployeeMapper extends BaseMapper<HrEmployee> {

    /**
     * Page query with department name joined.
     * DataScope filtering is handled by DataScopeInterceptor.
     */
    IPage<HrEmployee> selectPageWithDept(IPage<HrEmployee> page,
                                         @Param("keyword") String keyword,
                                         @Param("status") String status);
}
