package com.hrms.common.payroll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.common.payroll.entity.PyBankPaymentDetail;
import org.apache.ibatis.annotations.Mapper;

/**
 * 银行代发明细Mapper接口
 */
@Mapper
public interface PyBankPaymentDetailMapper extends BaseMapper<PyBankPaymentDetail> {
}
