package com.hrms.common.attendance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.BizCode;
import com.hrms.common.attendance.dto.ShiftCreateDto;
import com.hrms.common.attendance.entity.AtSchedule;
import com.hrms.common.attendance.entity.AtShift;
import com.hrms.common.attendance.mapper.AtScheduleMapper;
import com.hrms.common.attendance.mapper.AtShiftMapper;
import com.hrms.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for shift template CRUD.
 */
@Service
@RequiredArgsConstructor
public class ShiftService {

    private final AtShiftMapper shiftMapper;
    private final AtScheduleMapper scheduleMapper;

    @Transactional
    public AtShift create(ShiftCreateDto dto) {
        AtShift shift = new AtShift();
        shift.setShiftName(dto.getShiftName());
        shift.setStartTime(dto.getStartTime());
        shift.setEndTime(dto.getEndTime());
        shift.setFlexMinutes(dto.getFlexMinutes());
        shift.setIsNightShift(dto.getIsNightShift());
        shift.setRemark(dto.getRemark());
        shiftMapper.insert(shift);
        return shift;
    }

    public IPage<AtShift> list(Page<AtShift> page) {
        return shiftMapper.selectPage(page,
                new LambdaQueryWrapper<AtShift>().orderByAsc(AtShift::getId));
    }

    @Transactional
    public AtShift update(Long id, ShiftCreateDto dto) {
        AtShift shift = shiftMapper.selectById(id);
        if (shift == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Shift not found: " + id);
        }
        shift.setShiftName(dto.getShiftName());
        shift.setStartTime(dto.getStartTime());
        shift.setEndTime(dto.getEndTime());
        shift.setFlexMinutes(dto.getFlexMinutes());
        shift.setIsNightShift(dto.getIsNightShift());
        shift.setRemark(dto.getRemark());
        shiftMapper.updateById(shift);
        return shift;
    }

    @Transactional
    public void delete(Long id) {
        // Check if shift is referenced in schedules
        long count = scheduleMapper.selectCount(
                new LambdaQueryWrapper<AtSchedule>().eq(AtSchedule::getShiftId, id));
        if (count > 0) {
            throw new BizException(BizCode.SHIFT_IN_USE,
                    "Shift is referenced by " + count + " schedule(s), cannot delete");
        }
        shiftMapper.deleteById(id);
    }
}
