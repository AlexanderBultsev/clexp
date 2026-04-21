package com.clexp.auth.service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import reactor.core.publisher.Mono;

@Service
public class JwtService {
    
    @Value("${jwt.secret}")
    private String secretKey;
    
    @Value("${jwt.access-token.expiration}")
    private long accessTokenExpiration;
    
    @Value("${jwt.refresh-token.expiration}")
    private long refreshTokenExpiration;
    
    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes());
    }
    
    public Mono<String> extractUsername(String token) {
        return Mono.fromCallable(() -> extractClaim(token, Claims::getSubject));
    }
    
    public Mono<UUID> extractUserId(String token) {
        return Mono.fromCallable(() -> 
            extractClaim(token, claims -> claims.get("userId", UUID.class))
        );
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
    
    public Mono<String> generateAccessToken(UserDetails userDetails, UUID userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return generateToken(claims, userDetails, accessTokenExpiration);
    }
    
    public Mono<String> generateRefreshToken(UserDetails userDetails, UUID userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        return generateToken(claims, userDetails, refreshTokenExpiration);
    }
    
    private Mono<String> generateToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Mono.fromCallable(() ->
            Jwts.builder()
                .setClaims(extraClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact()
        );
    }
    
    public Mono<Boolean> validateToken(String token, UserDetails userDetails) {
        return extractUsername(token)
            .map(username -> username.equals(userDetails.getUsername()) && !isTokenExpired(token))
            .onErrorReturn(false);
    }
    
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
}
