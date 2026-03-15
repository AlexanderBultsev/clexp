package com.clexp.user.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.clexp.common.dto.UserResponse;
import com.clexp.common.dto.UserUpdateRequest;
import com.clexp.user.security.CustomUserDetails;
import com.clexp.user.service.UserService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public Mono<ResponseEntity<UserResponse>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        return userService.getUser(userDetails.getUserId())
            .map(ResponseEntity::ok);
    }

    @GetMapping("/{userId}")
    public Mono<ResponseEntity<UserResponse>> getUserProfile(@PathVariable UUID userId) {
        return userService.getUser(userId)
            .map(ResponseEntity::ok);
    }

    @PatchMapping("/me")
    public Mono<ResponseEntity<UserResponse>> patchProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody UserUpdateRequest request) {
        
        return userService.updateUser(userDetails.getUserId(), request)
            .map(ResponseEntity::ok);
    }
}
