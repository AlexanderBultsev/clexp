package com.clexp.auth.security;

import java.util.Collections;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import com.clexp.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
 
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
