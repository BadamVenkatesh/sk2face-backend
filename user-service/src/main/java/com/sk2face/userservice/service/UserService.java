package com.sk2face.userservice.service;

import com.sk2face.userservice.dto.UserRequest;
import com.sk2face.userservice.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


public interface UserService {
    UserResponse createUser(UserRequest user);

    UserResponse getUserById(Long id);

    Page<UserResponse> getAllUsers(Pageable pageable);

    UserResponse updateUser(Long id, UserRequest request);

    void deactivateUser(Long id);
}
