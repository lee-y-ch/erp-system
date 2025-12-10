package com.erp.approval.request.controller;

import com.erp.approval.request.document.ApprovalRequestDoc;
import com.erp.approval.request.dto.ApprovalCreateRequest;
import com.erp.approval.request.service.ApprovalRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/approvals")
@RequiredArgsConstructor
public class ApprovalRequestController {

    private final ApprovalRequestService approvalRequestService;

    @PostMapping
    public ResponseEntity<Map<String, Integer>> createApproval(@RequestBody ApprovalCreateRequest request) {

        // DTO(요청 데이터) -> Document(DB 저장용) 변환
        List<ApprovalRequestDoc.ApprovalStep> steps = request.getSteps().stream()
                .map(stepDto -> ApprovalRequestDoc.ApprovalStep.builder()
                        .step(stepDto.getStep())
                        .approverId(stepDto.getApproverId())
                        .status("pending") // 초기 상태는 무조건 pending
                        .build())
                .collect(Collectors.toList());

        // 서비스 호출 (DB 저장 + gRPC 전송)
        Integer requestId = approvalRequestService.createApproval(
                request.getRequesterId(),
                request.getTitle(),
                request.getContent(),
                steps
        );

        // 결과 반환
        return ResponseEntity.ok(Map.of("requestId", requestId));
    }

    // 결재 요청 목록 조회 (GET /approvals)
    @GetMapping
    public ResponseEntity<List<ApprovalRequestDoc>> getAllApprovals() {
        return ResponseEntity.ok(approvalRequestService.getAllApprovals());
    }

    // 결재 요청 상세 조회 (GET /approvals/{requestId})
    @GetMapping("/{requestId}")
    public ResponseEntity<ApprovalRequestDoc> getApproval(@PathVariable Integer requestId) {
        return ResponseEntity.ok(approvalRequestService.getApproval(requestId));
    }
}
