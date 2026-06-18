package com.hrms.common.payroll.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hrms.common.payroll.entity.PyBankPaymentBatch;
import org.apache.ibatis.annotations.Mapper;

/**
 * 银行代发批次Mapper接口
 */
@Mapper
public interface PyBankPaymentBatchMapper extends BaseMapper<PyBankPaymentBatch> {
}
