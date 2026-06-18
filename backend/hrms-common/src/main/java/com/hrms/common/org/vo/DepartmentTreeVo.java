package com.hrms.common.org.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Tree node VO for department hierarchy.
 */
@Data
public class DepartmentTreeVo {

    private Long id;

    private Long companyId;

    private Long parentId;

    private String name;

    private String code;

    private String path;

    private Integer level;

    private Long headId;

    private Integer sortOrder;

    private List<DepartmentTreeVo> children = new ArrayList<>();
}
