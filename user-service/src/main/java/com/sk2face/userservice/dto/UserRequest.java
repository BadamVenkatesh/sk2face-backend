package com.sk2face.userservice.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRequest {
    private String userId;

    private String employeeId;

    private String fullName;

    private String officialEmail;

    private String designation;

    private String departmentName;

    private String phoneNumber;
}
