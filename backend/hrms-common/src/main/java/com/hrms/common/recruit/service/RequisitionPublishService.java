package com.hrms.common.recruit.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;
import com.hrms.common.recruit.dto.RequisitionPublishDto;
import com.hrms.common.recruit.entity.RcJobRequisition;
import com.hrms.common.recruit.entity.RcRequisitionPublish;
import com.hrms.common.recruit.mapper.RcJobRequisitionMapper;
import com.hrms.common.recruit.mapper.RcRequisitionPublishMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 职位一键发布服务。
 * <p>支持将一个职位需求同时发布到多个招聘渠道（官网/智联/BOSS等），并生成发布链接。</p>
 * <p>每个渠道生成独立的发布记录，状态为PUBLISHED，并生成模拟的发布URL。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RequisitionPublishService {

    private final RcRequisitionPublishMapper publishMapper;
    private final RcJobRequisitionMapper requisitionMapper;

    /** 渠道名称映射 */
    private static final Map<String, String> CHANNEL_DISPLAY_NAMES = Map.of(
            "OFFICIAL", "公司官网",
            "ZHILIAN", "智联招聘",
            "BOSS", "BOSS直聘",
            "LAGOUP", "拉勾网",
            "51JOB", "前程无忧"
    );

    /**
     * 一键发布职位到多个渠道。
     *
     * @param dto 发布请求（包含职位需求ID和渠道列表）
     * @return 各渠道发布记录列表
     */
    @Transactional
    public List<RcRequisitionPublish> publishToMultipleChannels(RequisitionPublishDto dto) {
        // 校验职位需求存在且状态为OPEN
        RcJobRequisition requisition = requisitionMapper.selectById(dto.getRequisitionId());
        if (requisition == null) {
            throw new BizException(BizCode.BAD_REQUEST, "职位需求不存在: " + dto.getRequisitionId());
        }
        if (!"OPEN".equals(requisition.getStatus())) {
            throw new BizException(BizCode.REQUISITION_CLOSED, "职位需求已关闭，无法发布: " + dto.getRequisitionId());
        }

        List<RcRequisitionPublish> results = new ArrayList<>();
        for (String channel : dto.getChannels()) {
            // 检查是否已在该渠道发布过
            Long existingCount = publishMapper.selectCount(
                    new LambdaQueryWrapper<RcRequisitionPublish>()
                            .eq(RcRequisitionPublish::getRequisitionId, dto.getRequisitionId())
                            .eq(RcRequisitionPublish::getChannel, channel)
                            .in(RcRequisitionPublish::getStatus, "PENDING", "PUBLISHED"));
            if (existingCount > 0) {
                log.warn("职位已在该渠道发布: requisitionId={}, channel={}", dto.getRequisitionId(), channel);
                continue;
            }

            RcRequisitionPublish publish = new RcRequisitionPublish();
            publish.setRequisitionId(dto.getRequisitionId());
            publish.setChannel(channel);
            publish.setPublishUrl(generatePublishUrl(channel, dto.getRequisitionId()));
            publish.setStatus("PUBLISHED");
            publish.setPublishedAt(LocalDateTime.now());
            publish.setViewCount(0);
            publish.setApplyCount(0);
            publishMapper.insert(publish);
            results.add(publish);

            log.info("职位发布成功: requisitionId={}, channel={}, url={}",
                    dto.getRequisitionId(), channel, publish.getPublishUrl());
        }

        return results;
    }

    /**
     * 查询职位需求的所有发布记录
     */
    public List<RcRequisitionPublish> listByRequisition(Long requisitionId) {
        return publishMapper.selectList(
                new LambdaQueryWrapper<RcRequisitionPublish>()
                        .eq(RcRequisitionPublish::getRequisitionId, requisitionId)
                        .orderByDesc(RcRequisitionPublish::getCreatedAt));
    }

    /**
     * 关闭某渠道的发布
     */
    @Transactional
    public void closePublish(Long publishId) {
        RcRequisitionPublish publish = publishMapper.selectById(publishId);
        if (publish == null) {
            throw new BizException(BizCode.BAD_REQUEST, "发布记录不存在: " + publishId);
        }
        publish.setStatus("CLOSED");
        publish.setClosedAt(LocalDateTime.now());
        publishMapper.updateById(publish);
        log.info("关闭职位发布: publishId={}, channel={}", publishId, publish.getChannel());
    }

    /**
     * 获取所有支持的渠道列表
     */
    public List<Map<String, String>> getSupportedChannels() {
        List<Map<String, String>> channels = new ArrayList<>();
        for (Map.Entry<String, String> entry : CHANNEL_DISPLAY_NAMES.entrySet()) {
            Map<String, String> channel = new HashMap<>();
            channel.put("code", entry.getKey());
            channel.put("name", entry.getValue());
            channels.add(channel);
        }
        return channels;
    }

    /**
     * 生成发布URL（模拟生成各渠道的发布链接）
     * <p>实际生产环境会调用各渠道的API，这里生成模拟URL</p>
     */
    private String generatePublishUrl(String channel, Long requisitionId) {
        String baseUrl = switch (channel) {
            case "OFFICIAL" -> "https://careers.example.com/jobs";
            case "ZHILIAN" -> "https://sou.zhaopin.com/jobs";
            case "BOSS" -> "https://www.zhipin.com/job_detail";
            case "LAGOUP" -> "https://www.lagou.com/jobs";
            case "51JOB" -> "https://search.51job.com/list";
            default -> "https://hrms.example.com/recruit/jobs";
        };
        return String.format("%s/%d?ref=hrms&channel=%s&t=%d",
                baseUrl, requisitionId, channel.toLowerCase(), System.currentTimeMillis());
    }
}
