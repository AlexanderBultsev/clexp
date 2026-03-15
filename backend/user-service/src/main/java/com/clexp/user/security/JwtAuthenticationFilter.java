package com.clexp.user.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {
    
    private final JwtService jwtService;
    private final ReactiveUserDetailsServiceImpl userDetailsService;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.debug("No token, continuing chain");
            return chain.filter(exchange);
        }
        
        String token = authHeader.substring(7);
        log.debug("Processing token for path: {}", exchange.getRequest().getPath().value());
        
        return jwtService.extractUsername(token)
        .flatMap(username -> 
            userDetailsService.findByUsername(username)
                .flatMap(userDetails -> 
                    jwtService.validateToken(token, userDetails)
                        .flatMap(isValid -> {
                            if (Boolean.TRUE.equals(isValid)) {
                                UsernamePasswordAuthenticationToken auth = 
                                    new UsernamePasswordAuthenticationToken(
                                        userDetails, 
                                        null, 
                                        userDetails.getAuthorities()
                                    );
                                return chain.filter(exchange)
                                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
                            }
                            return chain.filter(exchange);
                        })
                )
        )
        .onErrorResume(e -> {
            log.error("Token processing error", e);
            return chain.filter(exchange);
        })
        .switchIfEmpty(chain.filter(exchange));
    }
}