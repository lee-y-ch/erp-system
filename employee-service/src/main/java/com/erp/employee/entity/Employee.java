package com.erp.employee.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "employees") // DB 테이블 이름 지정
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 기본 생성자
@AllArgsConstructor // 테스트용 전체 생성자
@Builder // 객체 생성을 쉽게 해주는 패턴
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // AUTO_INCREMENT 설정
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 100)
    private String department;

    @Column(nullable = false, length = 100)
    private String position;

    @CreationTimestamp // INSERT 시 현재 시간 자동 저장
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 정보 수정을 위한 메소드 (Setter 대신 사용)
    public void update(String department, String position) {
        this.department = department;
        this.position = position;
    }
}