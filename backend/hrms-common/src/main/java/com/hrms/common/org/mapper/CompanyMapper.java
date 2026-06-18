package com.hrms.common.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.common.org.entity.Company;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for company.
 */
@Mapper
public interface CompanyMapper extends BaseMapper<Company> {
}
