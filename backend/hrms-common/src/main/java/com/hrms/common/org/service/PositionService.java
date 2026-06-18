package com.hrms.common.org.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;
import com.hrms.common.org.dto.PositionDto;
import com.hrms.common.org.entity.Position;
import com.hrms.common.org.mapper.PositionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for position CRUD and headcount operations.
 */
@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionMapper positionMapper;

    public List<Position> list(Long deptId) {
        LambdaQueryWrapper<Position> qw = new LambdaQueryWrapper<>();
        if (deptId != null) {
            qw.eq(Position::getDeptId, deptId);
        }
        return positionMapper.selectList(qw);
    }

    public Position getById(Long id) {
        Position pos = positionMapper.selectById(id);
        if (pos == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Position not found: " + id);
        }
        return pos;
    }

    @Transactional
    public Position create(PositionDto dto) {
        Position pos = new Position();
        pos.setDeptId(dto.getDeptId());
        pos.setJobId(dto.getJobId());
        pos.setName(dto.getName());
        pos.setCode(dto.getCode());
        pos.setHeadcount(dto.getHeadcount() != null ? dto.getHeadcount() : 1);
        pos.setOccupied(0);
        positionMapper.insert(pos);
        return pos;
    }

    @Transactional
    public Position update(Long id, PositionDto dto) {
        Position pos = getById(id);
        pos.setName(dto.getName());
        pos.setCode(dto.getCode());
        if (dto.getHeadcount() != null) {
            pos.setHeadcount(dto.getHeadcount());
        }
        positionMapper.updateById(pos);
        return pos;
    }

    @Transactional
    public void delete(Long id) {
        Position pos = getById(id);
        if (pos.getOccupied() != null && pos.getOccupied() > 0) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "Cannot delete position with active employees (occupied=" + pos.getOccupied() + ")");
        }
        positionMapper.deleteById(id);
    }

    /**
     * Increment occupied count.
     * Called when an employee is onboarded.
     *
     * @return the updated position
     */
    @Transactional
    public Position incrOccupied(Long positionId) {
        Position pos = getById(positionId);
        int current = pos.getOccupied() != null ? pos.getOccupied() : 0;
        int limit = pos.getHeadcount() != null ? pos.getHeadcount() : 1;
        if (current >= limit) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "Position headcount is full (occupied=" + current + ", headcount=" + limit + ")");
        }
        pos.setOccupied(current + 1);
        positionMapper.updateById(pos);
        return pos;
    }

    /**
     * Decrement occupied count.
     * Called when an employee offboards.
     */
    @Transactional
    public Position decrOccupied(Long positionId) {
        Position pos = getById(positionId);
        int current = pos.getOccupied() != null ? pos.getOccupied() : 0;
        if (current <= 0) {
            throw new BizException(BizCode.BAD_REQUEST, "Position occupied count is already 0");
        }
        pos.setOccupied(current - 1);
        positionMapper.updateById(pos);
        return pos;
    }
}
