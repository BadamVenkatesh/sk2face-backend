package com.sk2face.authservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "sk2face-user-service")
public interface UserServiceClient {

    @PostMapping("/api/users")
    Object createUser(@RequestBody UserRequest userRequest);
}
