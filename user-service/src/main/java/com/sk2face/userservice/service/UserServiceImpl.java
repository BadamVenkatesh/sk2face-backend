package com.sk2face.userservice.service;

import com.sk2face.userservice.dto.UserRequest;
import com.sk2face.userservice.dto.UserResponse;
import com.sk2face.userservice.entity.UserEntity;
import com.sk2face.userservice.exception.UserNotFoundException;
import com.sk2face.userservice.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserResponse createUser(UserRequest request) {

        UserEntity user = new UserEntity();
        user.setEmployeeId(request.getEmployeeId());
        user.setFullName(request.getFullName());
        user.setOfficialEmail(request.getOfficalEmail());
        user.setDesignation(request.getDesignation());
        user.setDepartmentName(request.getDepartmentName());
        user.setPhoneNumber(request.getPhoneNumber());

        userRepository.save(user);

        return mapToResponse(user);
    }

    @Override
    public UserResponse getUserById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        return mapToResponse(user);
    }

    @Override
    public Page<UserResponse> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Override
    public UserResponse updateUser(Long id, UserRequest request) {

        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));

        if (request.getFullName() != null)
            user.setFullName(request.getFullName());

        if (request.getDesignation() != null)
            user.setDesignation(request.getDesignation());

        if (request.getDepartmentName() != null)
            user.setDepartmentName(request.getDepartmentName());

        if (request.getPhoneNumber() != null)
            user.setPhoneNumber(request.getPhoneNumber());

        user.setUpdatedAt(LocalDateTime.now());

        return mapToResponse(user);
    }

    @Override
    public void deactivateUser(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
        userRepository.deleteById(user.getUserId());
    }

    private UserResponse mapToResponse(UserEntity user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .employeeId(user.getEmployeeId())
                .fullName(user.getFullName())
                .designation(user.getDesignation())
                .departmentName(user.getDepartmentName())
                .officialEmail(user.getOfficialEmail())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }
}
