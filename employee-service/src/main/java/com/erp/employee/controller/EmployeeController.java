package com.erp.employee.controller;

import com.erp.employee.dto.EmployeeRequest;
import com.erp.employee.dto.EmployeeResponse;
import com.erp.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    // 1. 직원 생성 (POST /employees)
    @PostMapping
    public ResponseEntity<Map<String, Long>> createEmployee(@RequestBody EmployeeRequest request) {
        Long id = employeeService.createEmployee(request);
        return ResponseEntity.ok(Map.of("id", id));
    }

    // 2. 직원 목록 조회 (GET /employees)
    @GetMapping
    public ResponseEntity<List<EmployeeResponse>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    // 3. 직원 상세 조회 (GET /employees/{id})
    @GetMapping("/{id}")
    public ResponseEntity<EmployeeResponse> getEmployee(@PathVariable Long id) {
        return ResponseEntity.ok(employeeService.getEmployee(id));
    }

    // 4. 직원 수정 (PUT /employees/{id})
    @PutMapping("/{id}")
    public ResponseEntity<Void> updateEmployee(@PathVariable Long id, @RequestBody EmployeeRequest request) {
        employeeService.updateEmployee(id, request);
        return ResponseEntity.ok().build();
    }

    // 5. 직원 삭제 (DELETE /employees/{id})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteEmployee(@PathVariable Long id) {
        employeeService.deleteEmployee(id);
        return ResponseEntity.ok().build();
    }
}
