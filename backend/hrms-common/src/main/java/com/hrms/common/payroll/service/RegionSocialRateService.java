package com.hrms.common.payroll.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;
import com.hrms.common.payroll.entity.PyRegionSocialRate;
import com.hrms.common.payroll.mapper.PyRegionSocialRateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 多地区社保公积金政策管理服务。
 * <p>不同城市有不同的社保基数/比例，员工按参保城市匹配对应政策。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegionSocialRateService {

    private final PyRegionSocialRateMapper regionRateMapper;

    /**
     * 创建地区社保政策
     */
    @Transactional
    public PyRegionSocialRate create(PyRegionSocialRate rate) {
        // 检查该城市是否已存在启用的政策
        Long count = regionRateMapper.selectCount(
                new LambdaQueryWrapper<PyRegionSocialRate>()
                        .eq(PyRegionSocialRate::getCity, rate.getCity())
                        .eq(PyRegionSocialRate::getEnabled, true));
        if (count > 0) {
            throw new BizException(BizCode.BAD_REQUEST, "该城市已存在启用的社保政策: " + rate.getCity());
        }
        rate.setEnabled(true);
        regionRateMapper.insert(rate);
        log.info("创建地区社保政策: city={}", rate.getCity());
        return rate;
    }

    /**
     * 更新地区社保政策
     */
    @Transactional
    public PyRegionSocialRate update(Long id, PyRegionSocialRate rate) {
        PyRegionSocialRate existing = regionRateMapper.selectById(id);
        if (existing == null) {
            throw new BizException(BizCode.BAD_REQUEST, "社保政策不存在: " + id);
        }
        // 如果修改了城市名，检查新城市是否冲突
        if (rate.getCity() != null && !rate.getCity().equals(existing.getCity())) {
            Long count = regionRateMapper.selectCount(
                    new LambdaQueryWrapper<PyRegionSocialRate>()
                            .eq(PyRegionSocialRate::getCity, rate.getCity())
                            .eq(PyRegionSocialRate::getEnabled, true)
                            .ne(PyRegionSocialRate::getId, id));
            if (count > 0) {
                throw new BizException(BizCode.BAD_REQUEST, "目标城市已存在启用的社保政策: " + rate.getCity());
            }
            existing.setCity(rate.getCity());
        }
        if (rate.getPensionPersonal() != null) existing.setPensionPersonal(rate.getPensionPersonal());
        if (rate.getPensionCompany() != null) existing.setPensionCompany(rate.getPensionCompany());
        if (rate.getMedicalPersonal() != null) existing.setMedicalPersonal(rate.getMedicalPersonal());
        if (rate.getMedicalFixedFee() != null) existing.setMedicalFixedFee(rate.getMedicalFixedFee());
        if (rate.getMedicalCompany() != null) existing.setMedicalCompany(rate.getMedicalCompany());
        if (rate.getUnemploymentPersonal() != null) existing.setUnemploymentPersonal(rate.getUnemploymentPersonal());
        if (rate.getUnemploymentCompany() != null) existing.setUnemploymentCompany(rate.getUnemploymentCompany());
        if (rate.getInjuryCompany() != null) existing.setInjuryCompany(rate.getInjuryCompany());
        if (rate.getMaternityCompany() != null) existing.setMaternityCompany(rate.getMaternityCompany());
        if (rate.getHousingFundPersonal() != null) existing.setHousingFundPersonal(rate.getHousingFundPersonal());
        if (rate.getHousingFundCompany() != null) existing.setHousingFundCompany(rate.getHousingFundCompany());
        if (rate.getSocialBaseFloor() != null) existing.setSocialBaseFloor(rate.getSocialBaseFloor());
        if (rate.getSocialBaseCeil() != null) existing.setSocialBaseCeil(rate.getSocialBaseCeil());
        if (rate.getFundBaseFloor() != null) existing.setFundBaseFloor(rate.getFundBaseFloor());
        if (rate.getFundBaseCeil() != null) existing.setFundBaseCeil(rate.getFundBaseCeil());
        if (rate.getEnabled() != null) existing.setEnabled(rate.getEnabled());

        regionRateMapper.updateById(existing);
        log.info("更新地区社保政策: id={}, city={}", id, existing.getCity());
        return existing;
    }

    /**
     * 根据城市获取启用的社保政策
     */
    public PyRegionSocialRate getByCity(String city) {
        return regionRateMapper.selectOne(
                new LambdaQueryWrapper<PyRegionSocialRate>()
                        .eq(PyRegionSocialRate::getCity, city)
                        .eq(PyRegionSocialRate::getEnabled, true));
    }

    /**
     * 获取所有启用的地区社保政策
     */
    public List<PyRegionSocialRate> listAll() {
        return regionRateMapper.selectList(
                new LambdaQueryWrapper<PyRegionSocialRate>()
                        .eq(PyRegionSocialRate::getEnabled, true)
                        .orderByAsc(PyRegionSocialRate::getCity));
    }

    /**
     * 删除（禁用）地区社保政策
     */
    @Transactional
    public void disable(Long id) {
        PyRegionSocialRate rate = regionRateMapper.selectById(id);
        if (rate == null) {
            throw new BizException(BizCode.BAD_REQUEST, "社保政策不存在: " + id);
        }
        rate.setEnabled(false);
        regionRateMapper.updateById(rate);
        log.info("禁用地区社保政策: id={}, city={}", id, rate.getCity());
    }
}
