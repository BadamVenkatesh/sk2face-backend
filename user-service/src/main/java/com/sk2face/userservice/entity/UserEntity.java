package com.sk2face.userservice.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "forensic_users")
@Data
public class UserEntity {

    @Id
    private String userId;
    private String employeeId;
    private String fullName;
    private String designation;
    private String departmentName;
    private String officialEmail;
    private String phoneNumber;
    private String macAddress;
    private Integer totalCasesSolved = 0;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}