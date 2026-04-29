package com.clexp.user.controller;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.clexp.auth.security.CustomUserDetails;
import com.clexp.common.dto.InterestDto;
import com.clexp.common.dto.LanguageDto;
import com.clexp.user.dto.ShortUserResponse;
import com.clexp.user.dto.UserResponse;
import com.clexp.user.dto.UserUpdateRequest;
import com.clexp.user.service.UserService;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Profile

    @GetMapping("/users/me")
    public Mono<UserResponse> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        return userService.getUserById(userDetails.getUserId(), userDetails.getUserId());
    }

    @GetMapping("/users/{userId}")
    public Mono<UserResponse> getUser(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        return userService.getUserById(userDetails.getUserId(), userId);
    }

    @GetMapping("/users/{userId}/short")
    public Mono<ShortUserResponse> getShortUser(
            @PathVariable UUID userId) {

        return userService.getShortUserById(userId);
    }

    @PutMapping("/users/me")
    public Mono<UserResponse> updateProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Validated @RequestBody UserUpdateRequest request) {
        
        return userService.updateUser(userDetails.getUserId(), request);
    }

    // Subscriptions

    @PostMapping("/users/{userId}/subscribe")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<Void> subscribe(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return userService.subscribe(userDetails.getUserId(), userId);
    }

    @DeleteMapping("/users/{userId}/subscribe")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> unsubscribe(
            @PathVariable UUID userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        return userService.unsubscribe(userDetails.getUserId(), userId);
    }

    // Languages
    @GetMapping("/languages")
    public Flux<LanguageDto> getLanguages() {
        return userService.getLanguages();
    }

    // Interests
    @GetMapping("/interests")
    public Flux<InterestDto> getinterests() {
        return userService.getInterests();
    }
}
