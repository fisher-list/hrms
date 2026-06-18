package com.hrms.common.org.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.common.org.entity.Job;
import org.apache.ibatis.annotations.Mapper;

/**
 * MyBatis-Plus mapper for job.
 */
@Mapper
public interface JobMapper extends BaseMapper<Job> {
}
