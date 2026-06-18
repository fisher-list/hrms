package com.hrms.common.employee.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.employee.dto.HandoverItemCreateDto;
import com.hrms.common.employee.entity.HrHandoverItem;
import com.hrms.common.employee.entity.HrTerminationForm;
import com.hrms.common.employee.mapper.HrHandoverItemMapper;
import com.hrms.common.employee.mapper.HrTerminationFormMapper;
import com.hrms.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 离职工作交接管理。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HandoverItemService {

    private final HrHandoverItemMapper handoverItemMapper;
    private final HrTerminationFormMapper terminationFormMapper;

    /**
     * 创建交接项。
     */
    @Transactional
    public HrHandoverItem create(HandoverItemCreateDto dto) {
        // 验证离职单存在
        HrTerminationForm form = terminationFormMapper.selectById(dto.getFormId());
        if (form == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Termination form not found: " + dto.getFormId());
        }

        HrHandoverItem item = new HrHandoverItem();
        item.setFormId(dto.getFormId());
        item.setCategory(dto.getCategory());
        item.setDescription(dto.getDescription());
        item.setHandoverTo(dto.getHandoverTo());
        item.setRemark(dto.getRemark());
        item.setStatus("PENDING");
        handoverItemMapper.insert(item);
        return item;
    }

    /**
     * 查询某离职单的交接清单。
     */
    public List<HrHandoverItem> listByFormId(Long formId) {
        return handoverItemMapper.selectList(
                new LambdaQueryWrapper<HrHandoverItem>()
                        .eq(HrHandoverItem::getFormId, formId)
                        .orderByAsc(HrHandoverItem::getCreatedAt));
    }

    /**
     * 更新交接项状态。
     */
    @Transactional
    public void updateStatus(Long id, String status) {
        HrHandoverItem item = handoverItemMapper.selectById(id);
        if (item == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Handover item not found: " + id);
        }
        item.setStatus(status);
        handoverItemMapper.updateById(item);
    }
}
