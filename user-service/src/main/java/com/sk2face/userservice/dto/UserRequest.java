package com.sk2face.userservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UserRequest {
    @NotBlank
    private String employeeId;

    @NotBlank
    private String fullName;

    @NotBlank
    private String officialEmail;

    @NotBlank
    private String designation;

    @NotBlank
    private String departmentName;

    @NotBlank
    private String phoneNumber;
}
