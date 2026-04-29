package com.clexp.auth.security;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import com.clexp.auth.service.JwtService;
import com.clexp.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {
    
    private final JwtService jwtService;
    private final ReactiveUserDetailsServiceImpl userDetailsService;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();

        if (isPublicEndpoint(path)) {
            return chain.filter(exchange);
        }
        
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No token");
            return Mono.error(new BusinessException("Missing token", HttpStatus.UNAUTHORIZED));
        }
        
        String token = authHeader.substring(7);
        log.debug("Authenticating request to: {}", path);
        
        return jwtService.extractUsername(token)
        .flatMap(username -> 
            userDetailsService.findByUsername(username)
                .switchIfEmpty(Mono.error(new BusinessException("User not found", HttpStatus.UNAUTHORIZED)))
                .flatMap(userDetails -> 
                    jwtService.validateToken(token, userDetails)
                        .flatMap(isValid -> {
                            if (Boolean.TRUE.equals(isValid)) {
                                Authentication auth = new UsernamePasswordAuthenticationToken(
                                    userDetails,
                                    token,
                                    userDetails.getAuthorities()
                                );
                                return chain.filter(exchange)
                                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                            } else {
                                return Mono.error(new BusinessException("Invalid token", HttpStatus.UNAUTHORIZED));
                            }
                        })
                )
        );
    }

    private boolean isPublicEndpoint(String path) {
        return path.startsWith("/api/auth/");
    }
}