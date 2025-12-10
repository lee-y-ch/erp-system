package com.erp.approval.processing.repository;

import com.erp.approval.processing.dto.ProcessingRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Repository
public class InMemoryApprovalRepository {

    // Key: Approver ID (결재자), Value: 결재 대기 목록 List
    // 동시성 문제를 예방하기 위해 ConcurrentHashMap 사용
    private final ConcurrentHashMap<Integer, List<ProcessingRequest>> approvalQueue = new ConcurrentHashMap<>();

    // 결재 요청 저장 (특정 결재자의 대기열에 추가)
    public void save(Integer approverId, ProcessingRequest request) {
        approvalQueue.computeIfAbsent(approverId, k -> new CopyOnWriteArrayList<>()).add(request);
        System.out.println("Saved request " + request.getRequestId() + " for approver " + approverId);
    }

    // 특정 결재자의 대기 목록 조회
    public List<ProcessingRequest> findAllByApproverId(Integer approverId) {
        return approvalQueue.getOrDefault(approverId, new ArrayList<>());
    }

    // 특정 결재자의 대기 목록에서 특정 요청 찾기
    public ProcessingRequest findByApproverIdAndRequestId(Integer approverId, Integer requestId) {
        List<ProcessingRequest> list = approvalQueue.get(approverId);
        if (list == null) return null;

        return list.stream()
                .filter(req -> req.getRequestId().equals(requestId))
                .findFirst()
                .orElse(null);
    }

    // 대기 목록에서 요청 삭제 (결재 처리 후)
    public void delete(Integer approverId, ProcessingRequest request) {
        List<ProcessingRequest> list = approvalQueue.get(approverId);
        if (list != null) {
            list.remove(request);
        }
    }
}
