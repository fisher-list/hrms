package com.hrms.common.performance.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;
import com.hrms.common.performance.entity.PfScoringItem;
import com.hrms.common.performance.entity.PfTemplate;
import com.hrms.common.performance.mapper.PfScoringItemMapper;
import com.hrms.common.performance.mapper.PfTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Performance template management: create with scoring items, list, get by id.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TemplateService {

    private final PfTemplateMapper templateMapper;
    private final PfScoringItemMapper scoringItemMapper;

    /**
     * Create a template with scoring items. Validates weight sum = 100.
     */
    @Transactional
    public PfTemplate create(String name, String description, List<PfScoringItem> items) {
        if (items == null || items.isEmpty()) {
            throw new BizException(BizCode.BAD_REQUEST, "At least one scoring item is required");
        }

        int weightSum = items.stream().mapToInt(PfScoringItem::getWeight).sum();
        if (weightSum != 100) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "Scoring item weights must sum to 100, got: " + weightSum);
        }

        PfTemplate template = new PfTemplate();
        template.setName(name);
        template.setDescription(description);
        templateMapper.insert(template);

        for (int i = 0; i < items.size(); i++) {
            PfScoringItem item = items.get(i);
            item.setTemplateId(template.getId());
            if (item.getSortOrder() == null) {
                item.setSortOrder(i + 1);
            }
            if (item.getMinScore() == null) {
                item.setMinScore(1);
            }
            if (item.getMaxScore() == null) {
                item.setMaxScore(5);
            }
            scoringItemMapper.insert(item);
        }

        log.info("Created performance template {} with {} scoring items", template.getId(), items.size());
        return template;
    }

    /**
     * List all templates.
     */
    public List<PfTemplate> list() {
        return templateMapper.selectList(
                new LambdaQueryWrapper<PfTemplate>()
                        .orderByDesc(PfTemplate::getCreatedAt));
    }

    /**
     * Get template by ID including scoring items.
     */
    public PfTemplate getById(Long id) {
        return templateMapper.selectById(id);
    }

    /**
     * Get scoring items for a template.
     */
    public List<PfScoringItem> getScoringItems(Long templateId) {
        return scoringItemMapper.selectList(
                new LambdaQueryWrapper<PfScoringItem>()
                        .eq(PfScoringItem::getTemplateId, templateId)
                        .orderByAsc(PfScoringItem::getSortOrder));
    }
}
