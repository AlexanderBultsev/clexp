package com.clexp.user.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clexp.auth.security.CustomUserDetails;
import com.clexp.user.dto.UserResponse;
import com.clexp.user.dto.UserUpdateRequest;
import com.clexp.user.service.UserService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/users/me")
    public Mono<UserResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        return userService.getUser(userDetails.getUserId());
    }

    @GetMapping("/users/{userId}")
    public Mono<UserResponse> getUserProfile(@PathVariable UUID userId) {
        return userService.getUser(userId);
    }

    @PutMapping("/users/me")
    public Mono<UserResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserUpdateRequest request) {
        
        return userService.updateUser(userDetails.getUserId(), request);
    }
}
