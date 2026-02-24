package com.sk2face.userservice.dto;

import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class UserResponse {
    private Long userId;
    private String employeeId;
    private String fullName;
    private String designation;
    private String departmentName;
    private String officialEmail;
    private String phoneNumber;
}
