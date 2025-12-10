package com.erp.employee.service;

import com.erp.employee.dto.EmployeeRequest;
import com.erp.employee.dto.EmployeeResponse;
import com.erp.employee.entity.Employee;
import com.erp.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;

    // 1. 직원 생성
    @Transactional
    public Long createEmployee(EmployeeRequest request) {
        Employee employee = Employee.builder()
                .name(request.getName())
                .department(request.getDepartment())
                .position(request.getPosition())
                .build();

        Employee savedEmployee = employeeRepository.save(employee);
        return savedEmployee.getId();
    }

    // 2. 직원 목록 조회 (필터링 기능 포함)
    @Transactional(readOnly = true)
    public List<EmployeeResponse> getAllEmployees() {
        // 실제로는 Query Parameter 처리가 필요하지만, 우선 전체 목록 반환으로 구현
        return employeeRepository.findAll().stream()
                .map(EmployeeResponse::new)
                .collect(Collectors.toList());
    }

    // 3. 직원 상세 조회
    @Transactional(readOnly = true)
    public EmployeeResponse getEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 직원이 존재하지 않습니다. id=" + id));
        return new EmployeeResponse(employee);
    }

    // 4. 직원 수정 (부서, 직급만 수정 가능)
    @Transactional
    public void updateEmployee(Long id, EmployeeRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 직원이 존재하지 않습니다. id=" + id));

        // Entity 내부의 update 메서드 사용 (Dirty Checking)
        employee.update(request.getDepartment(), request.getPosition());
    }

    // 5. 직원 삭제
    @Transactional
    public void deleteEmployee(Long id) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 직원이 존재하지 않습니다. id=" + id));
        employeeRepository.delete(employee);
    }
}