package com.hrms.common.employee.service;

import com.hrms.common.api.BizCode;
import com.hrms.common.employee.dto.EmployeeCreateDto;
import com.hrms.common.employee.entity.HrEmployee;
import com.hrms.common.employee.mapper.*;
import com.hrms.common.exception.BizException;
import com.hrms.common.org.entity.Position;
import com.hrms.common.org.mapper.PositionMapper;
import com.hrms.common.org.service.PositionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class HrEmployeeServiceTest {

    private HrEmployeeMapper employeeMapper;
    private HrEmployeeEducationMapper educationMapper;
    private HrEmployeeWorkExpMapper workExpMapper;
    private HrEmployeeFamilyMapper familyMapper;
    private HrEmployeeContractMapper contractMapper;
    private HrEmployeeBankAccountMapper bankAccountMapper;
    private HrEmployeeAddressMapper addressMapper;
    private PositionMapper positionMapper;
    private PositionService positionService;
    private EmployeeSubDataService subDataService;
    private EmployeeMasterService masterService;
    private HrEmployeeService employeeService;

    private HrEmployee sampleEmployee;

    @BeforeEach
    void setUp() {
        employeeMapper = mock(HrEmployeeMapper.class);
        educationMapper = mock(HrEmployeeEducationMapper.class);
        workExpMapper = mock(HrEmployeeWorkExpMapper.class);
        familyMapper = mock(HrEmployeeFamilyMapper.class);
        contractMapper = mock(HrEmployeeContractMapper.class);
        bankAccountMapper = mock(HrEmployeeBankAccountMapper.class);
        addressMapper = mock(HrEmployeeAddressMapper.class);
        positionMapper = mock(PositionMapper.class);

        // Use real PositionService with mocked mapper (avoids mocking concrete class)
        positionService = new PositionService(positionMapper);

        // Create the real sub-services
        subDataService = new EmployeeSubDataService(
                educationMapper, workExpMapper, familyMapper,
                contractMapper, bankAccountMapper, addressMapper);
        masterService = new EmployeeMasterService(employeeMapper, subDataService, positionService,
                null, null);
        employeeService = new HrEmployeeService(masterService);

        sampleEmployee = new HrEmployee();
        sampleEmployee.setId(1001L);
        sampleEmployee.setEmpNo("E001001");
        sampleEmployee.setName("Zhang San");
        sampleEmployee.setGender("M");
        sampleEmployee.setDeptId(201L);
        sampleEmployee.setPositionId(301L);
        sampleEmployee.setHireDate(LocalDate.of(2026, 1, 1));
        sampleEmployee.setStatus("ACTIVE");
    }

    private Position position(Long id, int headcount, int occupied) {
        Position p = new Position();
        p.setId(id);
        p.setHeadcount(headcount);
        p.setOccupied(occupied);
        p.setName("pos-" + id);
        p.setDeptId(201L);
        p.setJobId(301L);
        return p;
    }

    @Test
    @DisplayName("create() encrypts sensitive fields and generates empNo")
    void createEncryptsAndGeneratesEmpNo() {
        EmployeeCreateDto dto = new EmployeeCreateDto();
        dto.setName("Li Si");
        dto.setGender("F");
        dto.setIdCard("110101199001011234");
        dto.setPhone("13812345678");
        dto.setDeptId(201L);
        dto.setPositionId(301L);
        dto.setHireDate(LocalDate.of(2026, 6, 1));
        dto.setEmergencyPhone("13900001111");

        // Simulate insert setting the ID
        doAnswer(inv -> {
            HrEmployee emp = inv.getArgument(0);
            emp.setId(5001L);
            return 1;
        }).when(employeeMapper).insert(any(HrEmployee.class));

        // selectCount for duplicate check returns 0
        when(employeeMapper.selectCount(any())).thenReturn(0L);

        HrEmployee result = employeeService.create(dto);

        assertThat(result.getId()).isEqualTo(5001L);
        assertThat(result.getEmpNo()).isEqualTo("E005001");
        assertThat(result.getStatus()).isEqualTo("PENDING_HIRE");
        assertThat(result.getIdCardEnc()).isNotNull().isNotEqualTo("110101199001011234");
        assertThat(result.getPhoneEnc()).isNotNull().isNotEqualTo("13812345678");
        assertThat(result.getEmergencyPhoneEnc()).isNotNull().isNotEqualTo("13900001111");

        // Verify insert was called once for the employee, and updateById once for empNo
        verify(employeeMapper, times(1)).insert(any(HrEmployee.class));
        verify(employeeMapper).updateById(any(HrEmployee.class));
    }

    @Test
    @DisplayName("terminate() calls decrOccupied and sets TERMINATED status")
    void terminateReducesHeadcount() {
        Position pos = position(301L, 5, 3);
        when(employeeMapper.selectById(1001L)).thenReturn(sampleEmployee);
        when(positionMapper.selectById(301L)).thenReturn(pos);

        employeeService.terminate(1001L);

        assertThat(sampleEmployee.getStatus()).isEqualTo("TERMINATED");
        assertThat(pos.getOccupied()).isEqualTo(2);
        verify(employeeMapper).updateById(sampleEmployee);
    }

    @Test
    @DisplayName("terminate() rejects when status is not ACTIVE or ON_LEAVE")
    void terminateRejectsInvalidState() {
        sampleEmployee.setStatus("PROBATION");
        when(employeeMapper.selectById(1001L)).thenReturn(sampleEmployee);

        assertThatThrownBy(() -> employeeService.terminate(1001L))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(BizCode.EMPLOYEE_INVALID_STATE_TRANSITION);
    }

    @Test
    @DisplayName("terminate() rejects when status is already TERMINATED")
    void terminateRejectsAlreadyTerminated() {
        sampleEmployee.setStatus("TERMINATED");
        when(employeeMapper.selectById(1001L)).thenReturn(sampleEmployee);

        assertThatThrownBy(() -> employeeService.terminate(1001L))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(BizCode.EMPLOYEE_INVALID_STATE_TRANSITION);
    }

    // -- EmployeeStateMachine tests --

    @Test
    @DisplayName("State machine: PENDING_HIRE -> PROBATION is valid")
    void validTransitionPendingToProbation() {
        EmployeeStateMachine.validate("PENDING_HIRE", "PROBATION");
    }

    @Test
    @DisplayName("State machine: PROBATION -> ACTIVE is valid")
    void validTransitionProbationToActive() {
        EmployeeStateMachine.validate("PROBATION", "ACTIVE");
    }

    @Test
    @DisplayName("State machine: ACTIVE -> ON_LEAVE is valid")
    void validTransitionActiveToOnLeave() {
        EmployeeStateMachine.validate("ACTIVE", "ON_LEAVE");
    }

    @Test
    @DisplayName("State machine: ACTIVE -> TERMINATED is valid")
    void validTransitionActiveToTerminated() {
        EmployeeStateMachine.validate("ACTIVE", "TERMINATED");
    }

    @Test
    @DisplayName("State machine: ON_LEAVE -> ACTIVE is valid")
    void validTransitionOnLeaveToActive() {
        EmployeeStateMachine.validate("ON_LEAVE", "ACTIVE");
    }

    @Test
    @DisplayName("State machine: PENDING_HIRE -> ACTIVE is invalid")
    void invalidTransitionPendingToActive() {
        assertThatThrownBy(() -> EmployeeStateMachine.validate("PENDING_HIRE", "ACTIVE"))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(BizCode.EMPLOYEE_INVALID_STATE_TRANSITION);
    }

    @Test
    @DisplayName("State machine: TERMINATED -> ACTIVE is invalid (irreversible)")
    void invalidTransitionTerminatedToActive() {
        assertThatThrownBy(() -> EmployeeStateMachine.validate("TERMINATED", "ACTIVE"))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(BizCode.EMPLOYEE_INVALID_STATE_TRANSITION);
    }
}
