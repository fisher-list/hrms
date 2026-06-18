package com.hrms.common.rbac.datascope;

import com.hrms.common.org.service.DepartmentService;
import com.hrms.common.employee.mapper.HrEmployeeMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Provides department-tree queries for DataScope.
 *
 * <p>Delegates to {@link DepartmentService#getSubordinateDeptIds} which uses
 * a materialized-path LIKE query for efficient subtree retrieval.</p>
 */
@Service
@RequiredArgsConstructor
public class DepartmentTreeService {

    private final DepartmentService departmentService;
    private final HrEmployeeMapper hrEmployeeMapper;

    /**
     * Return all subordinate department ids (including the given dept itself).
     *
     * @param deptId root department id
     * @return list of department ids in the subtree; empty if deptId is null
     */
    public List<Long> getSubordinateDeptIds(Long deptId) {
        return departmentService.getSubordinateDeptIds(deptId);
    }

    /**
     * Look up the department ID for an employee record.
     *
     * @param employeeId the employee profile ID bound to the login account
     * @return the department ID, or null if the employee is not found
     */
    public Long getDeptIdForEmployee(Long employeeId) {
        if (employeeId == null) {
            return null;
        }
        var employee = hrEmployeeMapper.selectById(employeeId);
        return employee != null ? employee.getDeptId() : null;
    }
}
