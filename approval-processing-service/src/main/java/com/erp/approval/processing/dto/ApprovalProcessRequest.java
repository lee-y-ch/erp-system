package com.erp.approval.processing.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApprovalProcessRequest {
    private String status; // "approved" or "rejected"
}