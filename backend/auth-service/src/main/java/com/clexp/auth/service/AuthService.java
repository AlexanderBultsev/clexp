package com.clexp.auth.service;

import java.time.LocalDateTime;
import java.util.Collections;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.clexp.auth.dto.AuthResponse;
import com.clexp.auth.dto.LoginRequest;
import com.clexp.auth.dto.RegisterRequest;
import com.clexp.auth.repository.UserRepository;
import com.clexp.common.exception.BusinessException;
import com.clexp.common.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    
    @Transactional
    public Mono<AuthResponse> register(RegisterRequest request) {
        log.info("Registering new user with email: {}", request.getEmail());
        
        return Mono.zip(
                userRepository.existsByEmail(request.getEmail()),
                userRepository.existsByUsername(request.getUsername())
            )
            .flatMap(tuple -> {
                if (Boolean.TRUE.equals(tuple.getT1())) {
                    return Mono.error(new BusinessException("Email already registered", HttpStatus.CONFLICT));
                }
                if (Boolean.TRUE.equals(tuple.getT2())) {
                    return Mono.error(new BusinessException("Username already taken", HttpStatus.CONFLICT));
                }
                
                User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .passwordHash(passwordEncoder.encode(request.getPassword()))
                    .age(request.getAge())
                    .role(request.getRole())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .lastLoginAt(LocalDateTime.now())
                    .build();
                
                return userRepository.save(user)
                    .flatMap(savedUser -> 
                        Mono.zip(
                            jwtService.generateAccessToken(createUserDetails(savedUser), savedUser.getId()),
                            jwtService.generateRefreshToken(createUserDetails(savedUser), savedUser.getId())
                        ).map(tokens -> 
                            AuthResponse.builder()
                                .accessToken(tokens.getT1())
                                .refreshToken(tokens.getT2())
                                .build()
                        )
                    );
            });
    }
    
    public Mono<AuthResponse> login(LoginRequest request) {
        log.info("Login attempt for email: {}", request.getEmail());
        
        return userRepository.findByEmail(request.getEmail())
            .switchIfEmpty(Mono.error(new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED)))
            .flatMap(user -> {
                if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                    return Mono.error(new BusinessException("Invalid email or password", HttpStatus.UNAUTHORIZED));
                }
                
                user.setLastLoginAt(LocalDateTime.now());
                return userRepository.save(user)
                    .flatMap(savedUser ->
                        Mono.zip(
                            jwtService.generateAccessToken(createUserDetails(savedUser), savedUser.getId()),
                            jwtService.generateRefreshToken(createUserDetails(savedUser), savedUser.getId())
                        ).map(tokens ->
                            AuthResponse.builder()
                                .accessToken(tokens.getT1())
                                .refreshToken(tokens.getT2())
                                .build()
                        )
                    );
            });
    }
    
    private org.springframework.security.core.userdetails.User createUserDetails(User user) {
        return new org.springframework.security.core.userdetails.User(
            user.getEmail(),
            user.getPasswordHash(),
            Collections.singletonList(() -> "ROLE_USER")
        );
    }
}
