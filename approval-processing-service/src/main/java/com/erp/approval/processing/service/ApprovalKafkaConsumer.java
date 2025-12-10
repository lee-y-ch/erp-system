package com.erp.approval.processing.service;

import com.erp.approval.processing.document.ApprovalRequestDoc;
import com.erp.approval.processing.dto.ProcessingRequest; // 이 클래스 import 확인 (패키지명 다를 수 있음)
import com.erp.approval.processing.repository.InMemoryApprovalRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApprovalKafkaConsumer {

    private final InMemoryApprovalRepository repository;

    /**
     * Kafka 리스너
     * topics: 수신할 토픽 이름 (Producer가 보낸 것과 일치해야 함)
     * groupId: 컨슈머 그룹 ID (application.yml 설정과 일치해야 함)
     */
    @KafkaListener(topics = "approval-topic", groupId = "approval-group")
    public void consume(ApprovalRequestDoc doc) {
        log.info("Consumed message from Kafka: ID={}", doc.getRequestId());

        // 1. DTO(ApprovalRequestDoc) -> 도메인 객체(ProcessingRequest) 변환
        List<ProcessingRequest.Step> processingSteps = doc.getSteps().stream()
                .map(step -> ProcessingRequest.Step.builder()
                        .step(step.getStep())
                        .approverId(step.getApproverId())
                        .status(step.getStatus())
                        .build())
                .collect(Collectors.toList());

        ProcessingRequest processingRequest = ProcessingRequest.builder()
                .requestId(doc.getRequestId())
                .requesterId(doc.getRequesterId())
                .title(doc.getTitle())
                .content(doc.getContent())
                .steps(processingSteps)
                .build();

        // 2. 현재 결재해야 할 사람(Approver) 찾기 (status가 "pending"인 첫 번째 사람)
        Integer targetApproverId = null;
        for (ProcessingRequest.Step step : processingSteps) {
            if ("pending".equalsIgnoreCase(step.getStatus())) {
                targetApproverId = step.getApproverId();
                break;
            }
        }

        // 3. 메모리 저장소(Queue)에 저장
        if (targetApproverId != null) {
            repository.save(targetApproverId, processingRequest);
            log.info("Saved request {} to approver {}'s queue via Kafka", doc.getRequestId(), targetApproverId);
        } else {
            log.warn("No pending step found for request {}", doc.getRequestId());
        }
    }
}