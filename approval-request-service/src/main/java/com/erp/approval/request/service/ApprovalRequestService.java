package com.erp.approval.request.service;


import approval.ApprovalRequest;
import approval.ApprovalGrpc;
import com.erp.approval.request.document.ApprovalRequestDoc;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalRequestService {

    private final MongoTemplate mongoTemplate;

    // Stub 클래스 gRPC에 사용
    // @GrpcClient("approval-processing-service")
    // private ApprovalGrpc.ApprovalBlockingStub approvalStub;

    // Kafka 메시지 발송을 위한 템플릿 (Key: String, Value: Object)
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Transactional
    public Integer createApproval(Integer requesterId, String title, String content, List<ApprovalRequestDoc.ApprovalStep> steps) {
        // 1. MongoDB에 저장할 Document 생성
        int requestId = (int) (System.currentTimeMillis() & 0xfffffff);

        ApprovalRequestDoc doc = ApprovalRequestDoc.builder()
                .requestId(requestId)
                .requesterId(requesterId)
                .title(title)
                .content(content)
                .steps(steps)
                .finalStatus("in_progress")
                .createdAt(LocalDateTime.now())
                .build();

        // 2. MongoDB 저장
        mongoTemplate.save(doc);

        log.info("Saved request to MongoDB: {}", requestId);

        // gRPC 호출 대신 Kafka로 메시지 전송 (비동기)
        // 토픽: "approval-topic"
        // 키: requestId (String 변환) -> 같은 ID는 같은 파티션으로 가서 순서 보장
        // 값: doc (결재 문서 객체) -> JSON으로 직렬화되어 전송됨
        kafkaTemplate.send("approval-topic", String.valueOf(requestId), doc);

        log.info("Sent request to Kafka: {}", requestId);

        return requestId;

//        // 3. gRPC 요청 객체 생성
//        ApprovalRequest grpcRequest = ApprovalRequest.newBuilder()
//                .setRequestId(requestId)
//                .setRequesterId(requesterId)
//                .setTitle(title)
//                .setContent(content)
//                .addAllSteps(steps.stream()
//                        .map(step -> approval.Step.newBuilder()
//                                .setStep(step.getStep())
//                                .setApproverId(step.getApproverId())
//                                .setStatus("pending")
//                                .build())
//                        .collect(Collectors.toList()))
//                .build();
//
//        // 4. gRPC 호출
//        try {
//            //
//            approvalStub.requestApproval(grpcRequest);
//        } catch (Exception e) {
//            System.err.println("gRPC Call Failed: " + e.getMessage());
//        }
    }

    // 목록 조회
    public List<ApprovalRequestDoc> getAllApprovals() {
        return mongoTemplate.findAll(ApprovalRequestDoc.class);
    }

    // 상세 조회
    public ApprovalRequestDoc getApproval(Integer requestId) {
        Query query = new Query(Criteria.where("requestId").is(requestId));
        return mongoTemplate.findOne(query, ApprovalRequestDoc.class);
    }
}