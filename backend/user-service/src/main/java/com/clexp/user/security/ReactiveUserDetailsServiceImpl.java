package com.clexp.user.security;

import lombok.RequiredArgsConstructor;

import java.util.Collections;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import com.clexp.user.repository.UserRepository;
 
@Service
@RequiredArgsConstructor
public class ReactiveUserDetailsServiceImpl implements ReactiveUserDetailsService {
    
    private final UserRepository userRepository;
    
    @Override
    public Mono<UserDetails> findByUsername(String email) {
        return userRepository.findByEmail(email)
            .map(user -> new CustomUserDetails(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                Collections.singletonList(() -> "ROLE_USER")
            ));
    }
}
