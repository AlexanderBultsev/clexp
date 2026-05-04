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
public class WebSocketAuthenticationFilter implements WebFilter {
    
    private final JwtService jwtService;
    private final ReactiveUserDetailsServiceImpl userDetailsService;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        
        // Применяем фильтр только к WebSocket эндпоинтам
        if (!path.startsWith("/ws/")) {
            return chain.filter(exchange);
        }
        
        log.debug("WebSocket authentication for: {}", path);
        
        // Пытаемся получить токен из query параметра (для WebSocket удобнее)
        String token = extractToken(exchange);
        
        if (token == null) {
            log.warn("WebSocket connection attempt without token");
            return exchange.getResponse().setComplete();
        }
        
        return jwtService.extractUsername(token)
            .flatMap(username -> 
                userDetailsService.findByUsername(username)
                    .switchIfEmpty(Mono.defer(() -> {
                        log.warn("User not found for WebSocket connection: {}", username);
                        return Mono.error(new BusinessException("User not found", HttpStatus.UNAUTHORIZED));
                    }))
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
                                    log.warn("Invalid JWT token for WebSocket connection");
                                    return exchange.getResponse().setComplete();
                                }
                            })
                    )
            )
            .onErrorResume(error -> {
                log.error("Error authenticating WebSocket connection", error);
                return exchange.getResponse().setComplete();
            });
    }
    
    private String extractToken(ServerWebExchange exchange) {
        // Сначала пробуем получить из query параметра (для WebSocket удобнее)
        String token = exchange.getRequest().getQueryParams().getFirst("token");
        
        if (token != null) {
            return token;
        }
        
        // Если нет в query - пробуем из заголовка
        String bearerToken = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        
        return null;
    }
}