package com.sk2face.authservice.client;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserRequest {
    private String userId;
    private String employeeId;
    private String fullName;
    private String officialEmail;
    private String designation;
    private String departmentName;
    private String phoneNumber;
}
