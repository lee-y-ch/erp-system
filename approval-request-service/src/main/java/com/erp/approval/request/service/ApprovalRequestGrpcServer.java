package com.erp.approval.request.service;

import approval.*;
import com.erp.approval.request.document.ApprovalRequestDoc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.client.inject.GrpcClient;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@GrpcService
@RequiredArgsConstructor
public class ApprovalRequestGrpcServer extends ApprovalGrpc.ApprovalImplBase {

    private final MongoTemplate mongoTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    // [추가] 알림 서비스 주소를 설정 파일에서 가져옴 (기본값: 로컬)
    @Value("${notification.service.url:http://localhost:8084/notify}")
    private String notificationUrl;

    // 다음 결재자에게 넘기기 위해 다시 Processing Service를 호출할 클라이언트
    @GrpcClient("approval-processing-service")
    private ApprovalGrpc.ApprovalBlockingStub approvalStub;

    @Override
    public void returnApprovalResult(ApprovalResultRequest request, StreamObserver<ApprovalResultResponse> responseObserver) {
        int requestId = request.getRequestId();
        int step = request.getStep();
        String status = request.getStatus(); // approved or rejected

        System.out.println("Received Result for Request " + requestId + ": " + status);

        // 1. MongoDB에서 해당 요청 찾기
        Query query = new Query(Criteria.where("requestId").is(requestId));
        ApprovalRequestDoc doc = mongoTemplate.findOne(query, ApprovalRequestDoc.class);

        if (doc != null) {
            // 2. 해당 단계(Step)의 상태 업데이트
            List<ApprovalRequestDoc.ApprovalStep> steps = doc.getSteps();
            boolean isFinalStep = true; // 마지막 단계인지 확인용 플래그
            ApprovalRequestDoc.ApprovalStep nextStep = null;

            for (int i = 0; i < steps.size(); i++) {
                ApprovalRequestDoc.ApprovalStep s = steps.get(i);
                if (s.getStep() == step) {
                    // 현재 처리된 단계 상태 업데이트
                    steps.set(i, new ApprovalRequestDoc.ApprovalStep(s.getStep(), s.getApproverId(), status));
                }

                // 다음 단계가 있고, 그 상태가 pending이라면 찾기
                if (s.getStep() > step && "pending".equals(s.getStatus())) {
                    if (nextStep == null) nextStep = s; // 가장 먼저 만나는 다음 단계
                    isFinalStep = false;
                }
            }

            // DB 업데이트 (steps 배열 덮어쓰기 + updatedAt)
            Update update = new Update();
            update.set("steps", steps);
            update.set("updatedAt", LocalDateTime.now());

            // 3. 상태에 따른 분기 처리
            if ("rejected".equals(status)) {
                // 3-1. 반려된 경우: 최종 상태 REJECTED
                update.set("finalStatus", "rejected");
                System.out.println("Request " + requestId + " is REJECTED.");

                // 알림 메시지 구성
                String messageJson = String.format("{\"userId\": \"%d\", \"message\": \"Your request %d is %s\"}",
                        doc.getRequesterId(), requestId, status.toUpperCase());

                // Notification Service 호출 (Fire & Forget)
                try {
                    // RestTemplate을 빈으로 등록해서 써도 되지만 간단하게 직접 생성해서 사용
                    new org.springframework.web.client.RestTemplate().postForObject(
                            notificationUrl,
                            java.util.Map.of("userId", String.valueOf(doc.getRequesterId()), "message", "결재 상태 변경: " + status),
                            Void.class
                    );
                    System.out.println("Notification sent.");
                } catch (Exception e) {
                    System.err.println("Failed to send notification: " + e.getMessage());
                }

            } else if ("approved".equals(status)) {
                if (nextStep != null) {
                    // 3-2. 승인되었으나 다음 단계가 남은 경우: 다음 결재자에게 gRPC 요청 전송
                    System.out.println("Proceeding to next step: " + nextStep.getStep());
                    sendToNextApprover(doc, nextStep);

                } else {
                    // 3-3. 모든 결재 완료: 최종 상태 APPROVED
                    update.set("finalStatus", "approved");
                    System.out.println("Request " + requestId + " is FINALLY APPROVED.");

                    // 알림 메시지 구성
                    String messageJson = String.format("{\"userId\": \"%d\", \"message\": \"Your request %d is %s\"}",
                            doc.getRequesterId(), requestId, status.toUpperCase());

                    // Notification Service 호출 (Fire & Forget)
                    try {
                        // RestTemplate을 빈으로 등록해서 써도 되지만 간단하게 직접 생성해서 사용
                        new org.springframework.web.client.RestTemplate().postForObject(
                                notificationUrl,
                                java.util.Map.of("userId", String.valueOf(doc.getRequesterId()), "message", "결재 상태 변경: " + status),
                                Void.class
                        );
                        System.out.println("Notification sent.");
                    } catch (Exception e) {
                        System.err.println("Failed to send notification: " + e.getMessage());
                    }
                }
            }

            mongoTemplate.updateFirst(query, update, ApprovalRequestDoc.class);
        }

        // 4. 응답 전송
        ApprovalResultResponse response = ApprovalResultResponse.newBuilder()
                .setStatus("processed")
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    // 다음 결재자에게 요청 보내는 메소드 (재귀적 호출 구조)
    private void sendToNextApprover(ApprovalRequestDoc doc, ApprovalRequestDoc.ApprovalStep nextStep) {
        try {
            ApprovalRequest grpcRequest = ApprovalRequest.newBuilder()
                    .setRequestId(doc.getRequestId())
                    .setRequesterId(doc.getRequesterId())
                    .setTitle(doc.getTitle())
                    .setContent(doc.getContent())
                    .addAllSteps(doc.getSteps().stream()
                            .map(s -> approval.Step.newBuilder()
                                    .setStep(s.getStep())
                                    .setApproverId(s.getApproverId())
                                    .setStatus(s.getStatus()) // 현재 상태 그대로 전달
                                    .build())
                            .collect(Collectors.toList()))
                    .build();

            approvalStub.requestApproval(grpcRequest);
            System.out.println("Sent to next approver: " + nextStep.getApproverId());
        } catch (Exception e) {
            System.err.println("Failed to send to next approver: " + e.getMessage());
        }
    }
}