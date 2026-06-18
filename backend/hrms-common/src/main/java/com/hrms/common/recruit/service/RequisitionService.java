package com.hrms.common.recruit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;
import com.hrms.common.recruit.dto.RequisitionCreateDto;
import com.hrms.common.recruit.entity.RcJobRequisition;
import com.hrms.common.recruit.entity.RcOffer;
import com.hrms.common.recruit.mapper.RcJobRequisitionMapper;
import com.hrms.common.recruit.mapper.RcOfferMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Job requisition CRUD with auto-close when headcount is reached.
 */
@Service
@RequiredArgsConstructor
public class RequisitionService {

    private final RcJobRequisitionMapper requisitionMapper;
    private final RcOfferMapper offerMapper;

    @Transactional
    public RcJobRequisition create(RequisitionCreateDto dto) {
        RcJobRequisition entity = new RcJobRequisition();
        entity.setPositionId(dto.getPositionId());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setHeadcount(dto.getHeadcount());
        entity.setDeadline(dto.getDeadline());
        entity.setStatus("OPEN");
        requisitionMapper.insert(entity);
        return entity;
    }

    public RcJobRequisition getById(Long id) {
        RcJobRequisition req = requisitionMapper.selectById(id);
        if (req == null) {
            throw new BizException(BizCode.BAD_REQUEST, "职位需求不存在: " + id);
        }
        return req;
    }

    public IPage<RcJobRequisition> list(Page<RcJobRequisition> page) {
        return requisitionMapper.selectPage(page,
                new LambdaQueryWrapper<RcJobRequisition>()
                        .orderByDesc(RcJobRequisition::getCreatedAt));
    }

    /**
     * Check if requisition has reached headcount and auto-close if so.
     */
    @Transactional
    public void checkAndAutoClose(Long requisitionId) {
        RcJobRequisition req = requisitionMapper.selectById(requisitionId);
        if (req == null || !"OPEN".equals(req.getStatus())) {
            return;
        }
        Long acceptedCount = offerMapper.selectCount(
                new LambdaQueryWrapper<RcOffer>()
                        .eq(RcOffer::getJobRequisitionId, requisitionId)
                        .eq(RcOffer::getStatus, "ACCEPTED"));
        if (acceptedCount >= req.getHeadcount()) {
            req.setStatus("CLOSED");
            requisitionMapper.updateById(req);
        }
    }
}
