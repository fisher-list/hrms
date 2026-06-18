package com.hrms.common.org.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.hrms.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Company entity mapped to company table.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("company")
public class Company extends BaseEntity {

    private String code;

    private String name;

    private String legalRepresentative;

    private String address;

    private String phone;

    private String email;
}
