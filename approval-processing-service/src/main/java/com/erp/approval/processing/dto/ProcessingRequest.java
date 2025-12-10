package com.erp.approval.processing.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingRequest {
    private Integer requestId;
    private Integer requesterId;
    private String title;
    private String content;
    private List<Step> steps;

    @Getter
    @Setter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Step {
        private Integer step;
        private Integer approverId;
        private String status; // pending, approved, rejected
    }
}