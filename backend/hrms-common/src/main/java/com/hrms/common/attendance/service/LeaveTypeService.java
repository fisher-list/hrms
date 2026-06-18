package com.hrms.common.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.attendance.entity.AtLeaveType;
import com.hrms.common.attendance.mapper.AtLeaveTypeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for leave type CRUD.
 */
@Service
@RequiredArgsConstructor
public class LeaveTypeService {

    private final AtLeaveTypeMapper leaveTypeMapper;

    /**
     * List all active (non-deleted) leave types.
     */
    public List<AtLeaveType> list() {
        return leaveTypeMapper.selectList(
                new LambdaQueryWrapper<AtLeaveType>().orderByAsc(AtLeaveType::getId));
    }

    /**
     * Create a new leave type.
     */
    @Transactional
    public AtLeaveType create(AtLeaveType dto) {
        leaveTypeMapper.insert(dto);
        return dto;
    }
}
