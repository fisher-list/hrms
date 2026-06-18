package com.hrms.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Base entity carrying the 8 cross-cutting columns required by every business table.
 *
 * <p>See data-model §1.1.  {@code id} uses MyBatis-Plus snowflake assignment via
 * {@link IdType#ASSIGN_ID}.  Audit columns are populated by an automatic field-fill
 * handler (added in a later story); they are nullable here so unit tests can construct
 * entities without a Spring context.</p>
 */
@Data
public abstract class BaseEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableLogic(value = "false", delval = "true")
    private Boolean deleted;

    private Integer version;

    @TableField(fill = FieldFill.INSERT)
    private Long tenantId;
}
