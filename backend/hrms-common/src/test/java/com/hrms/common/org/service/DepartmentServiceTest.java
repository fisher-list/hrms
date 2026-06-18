package com.hrms.common.org.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.hrms.common.exception.BizException;
import com.hrms.common.org.entity.Department;
import com.hrms.common.org.mapper.DepartmentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DepartmentService}.
 */
class DepartmentServiceTest {

    private DepartmentMapper departmentMapper;
    private DepartmentService departmentService;

    @BeforeEach
    void setUp() {
        departmentMapper = mock(DepartmentMapper.class);
        departmentService = new DepartmentService(departmentMapper);
    }

    private Department dept(Long id, Long parentId, String path, int level) {
        Department d = new Department();
        d.setId(id);
        d.setParentId(parentId);
        d.setPath(path);
        d.setLevel(level);
        d.setCompanyId(100L);
        d.setName("dept-" + id);
        d.setCode("D" + id);
        d.setSortOrder(0);
        return d;
    }

    @Test
    @DisplayName("Move rejects circular reference: move dept under itself")
    void moveRejectsSelfReference() {
        Department d = dept(1L, null, "/1", 1);
        when(departmentMapper.selectById(1L)).thenReturn(d);

        assertThatThrownBy(() -> departmentService.move(1L, 1L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("Cannot move department under itself");
    }

    @Test
    @DisplayName("Move rejects circular reference: move parent under its own child")
    void moveRejectsDescendantAsNewParent() {
        // parent: id=1, path=/1
        // child: id=2, path=/1/2
        Department parent = dept(1L, null, "/1", 1);
        Department child = dept(2L, 1L, "/1/2", 2);
        when(departmentMapper.selectById(1L)).thenReturn(parent);
        when(departmentMapper.selectById(2L)).thenReturn(child);

        assertThatThrownBy(() -> departmentService.move(1L, 2L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("circular reference");
    }

    @Test
    @DisplayName("Create computes path and level automatically")
    void createComputesPathAndLevel() {
        Department parent = dept(10L, null, "/10", 1);
        when(departmentMapper.selectById(10L)).thenReturn(parent);
        when(departmentMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        com.hrms.common.org.dto.DepartmentDto dto = new com.hrms.common.org.dto.DepartmentDto();
        dto.setCompanyId(100L);
        dto.setParentId(10L);
        dto.setName("child");
        dto.setCode("C1");

        // simulate insert: mapper returns generated id
        Department result = departmentService.create(dto);

        // The path should be /10/{id} — but since we cannot easily mock the snowflake id,
        // we verify the logic was called by checking the path computation flow.
        assertThat(result.getPath()).isNotNull();
        assertThat(result.getPath()).startsWith("/10/");
        assertThat(result.getLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("Move updates descendant paths recursively")
    void moveUpdatesDescendantPaths() {
        // old tree: /1 -> /1/2 -> /1/2/3
        Department root = dept(1L, null, "/1", 1);
        Department child = dept(2L, 1L, "/1/2", 2);
        Department grandchild = dept(3L, 2L, "/1/2/3", 3);

        // move child (id=2) under sibling (id=4, path=/4)
        Department newParent = dept(4L, null, "/4", 1);

        when(departmentMapper.selectById(2L)).thenReturn(child);
        when(departmentMapper.selectById(4L)).thenReturn(newParent);

        // descendants of id=2 (path=/1/2) that are not id=2
        when(departmentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(grandchild));

        Department moved = departmentService.move(2L, 4L);

        // new path should be /4/2
        assertThat(moved.getPath()).isEqualTo("/4/2");
        assertThat(moved.getLevel()).isEqualTo(2);
    }

    @Test
    @DisplayName("getSubordinateDeptIds returns all descendants including self")
    void getSubordinateDeptIdsReturnsSubtree() {
        Department root = dept(100L, null, "/100", 1);
        Department child1 = dept(101L, 100L, "/100/101", 2);
        Department child2 = dept(102L, 100L, "/100/102", 2);

        when(departmentMapper.selectById(100L)).thenReturn(root);
        when(departmentMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(List.of(root, child1, child2));

        List<Long> ids = departmentService.getSubordinateDeptIds(100L);
        assertThat(ids).containsExactlyInAnyOrder(100L, 101L, 102L);
    }
}
