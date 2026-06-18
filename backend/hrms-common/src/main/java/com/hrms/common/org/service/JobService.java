package com.hrms.common.org.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;
import com.hrms.common.org.dto.JobDto;
import com.hrms.common.org.entity.Job;
import com.hrms.common.org.mapper.JobMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for job CRUD.
 */
@Service
@RequiredArgsConstructor
public class JobService {

    private final JobMapper jobMapper;

    public List<Job> list() {
        return jobMapper.selectList(
                new LambdaQueryWrapper<Job>()
                        .orderByAsc(Job::getSequence)
                        .orderByAsc(Job::getGrade));
    }

    public Job getById(Long id) {
        Job job = jobMapper.selectById(id);
        if (job == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Job not found: " + id);
        }
        return job;
    }

    @Transactional
    public Job create(JobDto dto) {
        Long existing = jobMapper.selectCount(
                new LambdaQueryWrapper<Job>().eq(Job::getCode, dto.getCode()));
        if (existing > 0) {
            throw new BizException(BizCode.BAD_REQUEST, "Job code already exists: " + dto.getCode());
        }
        validateSalary(dto);
        Job job = new Job();
        applyDto(job, dto);
        jobMapper.insert(job);
        return job;
    }

    @Transactional
    public Job update(Long id, JobDto dto) {
        Job job = getById(id);
        validateSalary(dto);
        applyDto(job, dto);
        jobMapper.updateById(job);
        return job;
    }

    @Transactional
    public void delete(Long id) {
        jobMapper.deleteById(id);
    }

    private void validateSalary(JobDto dto) {
        if (dto.getMaxSalary() != null && dto.getMinSalary() != null
                && dto.getMaxSalary().compareTo(dto.getMinSalary()) < 0) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "maxSalary must be >= minSalary");
        }
    }

    private void applyDto(Job job, JobDto dto) {
        job.setCode(dto.getCode());
        job.setName(dto.getName());
        job.setSequence(dto.getSequence());
        job.setGrade(dto.getGrade());
        job.setMinSalary(dto.getMinSalary());
        job.setMaxSalary(dto.getMaxSalary());
    }
}
