package com.erp.approval.processing.service;

import approval.ApprovalGrpc;
import approval.ApprovalResultRequest;
import approval.ApprovalResultResponse;
import com.erp.approval.processing.dto.ProcessingRequest;
import com.erp.approval.processing.repository.InMemoryApprovalRepository;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ApprovalCommandService {

    private final InMemoryApprovalRepository repository;

    // [핵심] 결과를 Request Service로 보내기 위한 gRPC 클라이언트
    @GrpcClient("approval-request-service")
    private ApprovalGrpc.ApprovalBlockingStub approvalStub;

    public void processApproval(Integer approverId, Integer requestId, String status) {
        // 1. 메모리에서 해당 요청 찾기
        ProcessingRequest request = repository.findByApproverIdAndRequestId(approverId, requestId);
        if (request == null) {
            throw new IllegalArgumentException("해당 결재 요청을 찾을 수 없습니다.");
        }

        // 2. 현재 단계 정보 찾기
        ProcessingRequest.Step currentStep = request.getSteps().stream()
                .filter(step -> step.getApproverId().equals(approverId))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("결재자 정보가 일치하지 않습니다."));

        // 3. 메모리에서 제거 (처리 완료되었으므로 대기열에서 삭제)
        repository.delete(approverId, request);
        System.out.println("Processed request " + requestId + " by " + approverId + ": " + status);

        // 4. gRPC로 결과 전송 (Request Service에게 알림)
        ApprovalResultRequest resultRequest = ApprovalResultRequest.newBuilder()
                .setRequestId(requestId)
                .setStep(currentStep.getStep())
                .setApproverId(approverId)
                .setStatus(status)
                .build();

        try {
            ApprovalResultResponse response = approvalStub.returnApprovalResult(resultRequest);
            System.out.println("Result sent to Request Service: " + response.getStatus());
        } catch (Exception e) {
            System.err.println("Failed to send result via gRPC: " + e.getMessage());
        }
    }
}