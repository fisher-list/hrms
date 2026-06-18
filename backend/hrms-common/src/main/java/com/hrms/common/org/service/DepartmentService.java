package com.hrms.common.org.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.api.BizCode;
import com.hrms.common.exception.BizException;
import com.hrms.common.org.dto.DepartmentDto;
import com.hrms.common.org.entity.Department;
import com.hrms.common.org.mapper.DepartmentMapper;
import com.hrms.common.org.vo.DepartmentTreeVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service layer for department CRUD and tree operations.
 */
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentMapper departmentMapper;

    /**
     * Build complete department tree for a company.
     * One SQL query fetches all departments, then Java assembles the tree.
     */
    public List<DepartmentTreeVo> tree(Long companyId) {
        List<Department> all = departmentMapper.selectList(
                new LambdaQueryWrapper<Department>()
                        .eq(Department::getCompanyId, companyId)
                        .orderByAsc(Department::getSortOrder)
                        .orderByAsc(Department::getId));

        List<DepartmentTreeVo> voList = all.stream()
                .map(this::toTreeVo)
                .collect(Collectors.toList());

        return assembleTree(voList);
    }

    @Transactional
    public Department create(DepartmentDto dto) {
        // auto-generate code when missing
        String code = (dto.getCode() == null || dto.getCode().isBlank())
                ? "DPT-" + dto.getCompanyId() + "-" + java.util.UUID.randomUUID().toString().substring(0, 8)
                : dto.getCode();
        // check unique code per company
        Long existing = departmentMapper.selectCount(
                new LambdaQueryWrapper<Department>()
                        .eq(Department::getCompanyId, dto.getCompanyId())
                        .eq(Department::getCode, code));
        if (existing > 0) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "Department code already exists in this company: " + code);
        }

        Department dept = new Department();
        dept.setCompanyId(dto.getCompanyId());
        dept.setParentId(dto.getParentId());
        dept.setName(dto.getName());
        dept.setCode(code);
        dept.setHeadId(dto.getHeadId());
        dept.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);

        // insert first to get the snowflake id, then compute path/level
        departmentMapper.insert(dept);

        String path = computePath(dept.getId(), dept.getParentId());
        int level = computeLevel(path);
        dept.setPath(path);
        dept.setLevel(level);
        departmentMapper.updateById(dept);

        return dept;
    }

    @Transactional
    public Department update(Long id, DepartmentDto dto) {
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Department not found: " + id);
        }
        dept.setName(dto.getName());
        dept.setHeadId(dto.getHeadId());
        dept.setSortOrder(dto.getSortOrder() != null ? dto.getSortOrder() : 0);
        departmentMapper.updateById(dept);
        return dept;
    }

    /**
     * Move a department under a new parent.
     * Validates against circular references and recursively updates all descendant paths.
     */
    @Transactional
    public Department move(Long id, Long newParentId) {
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Department not found: " + id);
        }

        // circular reference check
        if (newParentId != null) {
            if (newParentId.equals(id)) {
                throw new BizException(BizCode.BAD_REQUEST, "Cannot move department under itself");
            }
            // check if newParentId is a descendant of id
            Department newParent = departmentMapper.selectById(newParentId);
            if (newParent == null) {
                throw new BizException(BizCode.BAD_REQUEST, "New parent department not found: " + newParentId);
            }
            if (newParent.getPath() != null && dept.getPath() != null
                    && newParent.getPath().startsWith(dept.getPath())) {
                throw new BizException(BizCode.BAD_REQUEST,
                        "Cannot move department under its own descendant (circular reference)");
            }
        }

        String oldPath = dept.getPath();
        String newPath = computePath(id, newParentId);
        int newLevel = computeLevel(newPath);

        // update this department
        dept.setParentId(newParentId);
        dept.setPath(newPath);
        dept.setLevel(newLevel);
        departmentMapper.updateById(dept);

        // update all descendants: replace oldPath prefix with newPath
        List<Department> descendants = departmentMapper.selectList(
                new LambdaQueryWrapper<Department>()
                        .likeRight(Department::getPath, oldPath + "/")
                        .ne(Department::getId, id));
        for (Department desc : descendants) {
            String descNewPath = newPath + desc.getPath().substring(oldPath.length());
            desc.setPath(descNewPath);
            desc.setLevel(computeLevel(descNewPath));
            departmentMapper.updateById(desc);
        }

        return dept;
    }

    /**
     * Soft-delete a department after checking for active children and positions.
     */
    @Transactional
    public void delete(Long id) {
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new BizException(BizCode.BAD_REQUEST, "Department not found: " + id);
        }

        // check for active child departments
        Long childCount = departmentMapper.selectCount(
                new LambdaQueryWrapper<Department>()
                        .eq(Department::getParentId, id));
        if (childCount > 0) {
            throw new BizException(BizCode.BAD_REQUEST,
                    "Cannot delete department with active child departments");
        }

        // soft delete
        departmentMapper.deleteById(id);
    }

    /**
     * Get all subordinate department ids (including self) using path LIKE query.
     * Used by S02 DataScope.
     */
    public List<Long> getSubordinateDeptIds(Long deptId) {
        if (deptId == null) {
            return new ArrayList<>();
        }
        Department dept = departmentMapper.selectById(deptId);
        if (dept == null || dept.getPath() == null) {
            return List.of(deptId);
        }
        List<Department> subordinates = departmentMapper.selectList(
                new LambdaQueryWrapper<Department>()
                        .likeRight(Department::getPath, dept.getPath()));
        return subordinates.stream()
                .map(Department::getId)
                .collect(Collectors.toList());
    }

    // ---- internal helpers ----

    private String computePath(Long id, Long parentId) {
        if (parentId == null) {
            return "/" + id;
        }
        Department parent = departmentMapper.selectById(parentId);
        if (parent == null || parent.getPath() == null) {
            return "/" + id;
        }
        return parent.getPath() + "/" + id;
    }

    private int computeLevel(String path) {
        if (path == null) {
            return 1;
        }
        // count '/' characters; "/1" = level 1, "/1/2" = level 2
        int count = 0;
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) == '/') {
                count++;
            }
        }
        return count;
    }

    private DepartmentTreeVo toTreeVo(Department dept) {
        DepartmentTreeVo vo = new DepartmentTreeVo();
        vo.setId(dept.getId());
        vo.setCompanyId(dept.getCompanyId());
        vo.setParentId(dept.getParentId());
        vo.setName(dept.getName());
        vo.setCode(dept.getCode());
        vo.setPath(dept.getPath());
        vo.setLevel(dept.getLevel());
        vo.setHeadId(dept.getHeadId());
        vo.setSortOrder(dept.getSortOrder());
        return vo;
    }

    private List<DepartmentTreeVo> assembleTree(List<DepartmentTreeVo> all) {
        Map<Long, DepartmentTreeVo> map = all.stream()
                .collect(Collectors.toMap(DepartmentTreeVo::getId, v -> v));

        List<DepartmentTreeVo> roots = new ArrayList<>();
        for (DepartmentTreeVo vo : all) {
            if (vo.getParentId() == null) {
                roots.add(vo);
            } else {
                DepartmentTreeVo parent = map.get(vo.getParentId());
                if (parent != null) {
                    parent.getChildren().add(vo);
                }
            }
        }
        return roots;
    }
}
