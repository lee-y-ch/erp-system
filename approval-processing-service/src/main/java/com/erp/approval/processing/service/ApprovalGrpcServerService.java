package com.erp.approval.processing.service;

import approval.ApprovalGrpc;          // Stub 클래스
import approval.ApprovalRequest;       // 요청 메시지
import approval.ApprovalResponse;      // 응답 메시지
import com.erp.approval.processing.dto.ProcessingRequest;
import com.erp.approval.processing.repository.InMemoryApprovalRepository;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.List;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
public class ApprovalGrpcServerService extends ApprovalGrpc.ApprovalImplBase {

    private final InMemoryApprovalRepository repository;

    @Override
    public void requestApproval(ApprovalRequest request, StreamObserver<ApprovalResponse> responseObserver) {
        System.out.println("Received gRPC Request: ID=" + request.getRequestId());

        // 1. DTO 변환
        List<ProcessingRequest.Step> steps = request.getStepsList().stream()
                .map(step -> ProcessingRequest.Step.builder()
                        .step(step.getStep())
                        .approverId(step.getApproverId())
                        .status(step.getStatus())
                        .build())
                .collect(Collectors.toList());

        ProcessingRequest processingRequest = ProcessingRequest.builder()
                .requestId(request.getRequestId())
                .requesterId(request.getRequesterId())
                .title(request.getTitle())
                .content(request.getContent())
                .steps(steps)
                .build();

        // 2. 현재 결재해야 할 사람(Approver) 찾기 (status가 "pending"인 첫 번째 사람)
        Integer targetApproverId = null;
        for (ProcessingRequest.Step step : steps) {
            if ("pending".equalsIgnoreCase(step.getStatus())) {
                targetApproverId = step.getApproverId();
                break;
            }
        }

        // 3. 인메모리 저장소에 저장
        if (targetApproverId != null) {
            repository.save(targetApproverId, processingRequest);
        } else {
            System.out.println("No pending step found for request " + request.getRequestId());
        }

        // 4. 응답 전송
        ApprovalResponse response = ApprovalResponse.newBuilder()
                .setStatus("received")
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
