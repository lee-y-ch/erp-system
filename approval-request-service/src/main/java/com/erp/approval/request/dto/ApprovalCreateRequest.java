package com.erp.approval.request.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.List;

@Getter
@Setter
public class ApprovalCreateRequest {
    private Integer requesterId;
    private String title;
    private String content;
    private List<StepDto> steps; // 아래 내부 클래스 리스트


    @Getter
    @Setter
    public static class StepDto {
        private Integer step;
        private Integer approverId;
    }
}
