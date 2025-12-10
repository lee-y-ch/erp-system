package com.erp.approval.processing.controller;

import com.erp.approval.processing.dto.ApprovalProcessRequest;
import com.erp.approval.processing.dto.ProcessingRequest;
import com.erp.approval.processing.repository.InMemoryApprovalRepository;
import com.erp.approval.processing.service.ApprovalCommandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/process")
@RequiredArgsConstructor
public class ApprovalProcessingController {

    private final InMemoryApprovalRepository repository;
    private final ApprovalCommandService commandService;

    // 1. 대기 목록 조회 (GET /process/{approverId})
    @GetMapping("/{approverId}")
    public ResponseEntity<List<ProcessingRequest>> getPendingRequests(@PathVariable Integer approverId) {
        return ResponseEntity.ok(repository.findAllByApproverId(approverId));
    }

    // 2. 승인/반려 처리 (POST /process/{approverId}/{requestId})
    @PostMapping("/{approverId}/{requestId}")
    public ResponseEntity<Void> processApproval(
            @PathVariable Integer approverId,
            @PathVariable Integer requestId,
            @RequestBody ApprovalProcessRequest request) {

        commandService.processApproval(approverId, requestId, request.getStatus());
        return ResponseEntity.ok().build();
    }
}