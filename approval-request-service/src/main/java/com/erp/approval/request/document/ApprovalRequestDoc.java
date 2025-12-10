package com.erp.approval.request.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "approval_requests") // MongoDB 컬렉션 이름
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApprovalRequestDoc {

    @Id
    private String id; // MongoDB의 _id

    private Integer requestId; // 실제 비즈니스 로직용 ID
    private Integer requesterId; // 요청자 ID
    private String title;
    private String content;

    private List<ApprovalStep> steps; // 결재 단계 리스트

    private String finalStatus; // in_progress, approved, rejected

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // 내부 클래스로 Step 정의
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ApprovalStep {
        private Integer step;       // 단계 번호
        private Integer approverId; // 결재자 ID
        private String status;      // pending, approved, rejected
    }

    // 상태 업데이트 편의 메서드
    public void updateStatus(String status) {
        this.finalStatus = status;
    }
}