package com.erp.approval.processing.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApprovalRequestDoc {

    private String id;           // MongoDB ID (문자열)
    private Integer requestId;   // 업무 로직 ID (숫자)
    private Integer requesterId;
    private String title;
    private String content;
    private String finalStatus;
    private LocalDateTime createdAt;

    // 결재 단계 리스트
    private List<ApprovalStep> steps;

    // 내부 클래스로 Step 정의
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ApprovalStep {
        private Integer step;
        private Integer approverId;
        private String status; // pending, approved, rejected
    }
}