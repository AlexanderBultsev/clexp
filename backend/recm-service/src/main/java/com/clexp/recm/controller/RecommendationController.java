package com.clexp.recm.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.clexp.auth.security.CustomUserDetails;
import com.clexp.recm.service.PostRecommendationService;
import com.clexp.recm.service.UserRecommendationService;
import com.clexp.recm.service.UserRecommendationService.UserFeed;
import com.clexp.recm.service.PostRecommendationService.PostFeed;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.format.annotation.DateTimeFormat.ISO;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;


@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecommendationController {
    
    private final UserRecommendationService userRecmService;
    private final PostRecommendationService postRecmService;

    @GetMapping("/recm/posts")
    public Mono<PostFeed> getRecommendedPosts(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime cursor) {

        return postRecmService.getRecommendedPosts(userDetails.getUserId(), cursor);
    }
    
    @GetMapping("/recm/users")
    public Mono<UserFeed> getRecommendedUsers(@AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam(required = false) @DateTimeFormat(iso = ISO.DATE_TIME) LocalDateTime cursor) {

        return userRecmService.getRecommendedUsers(userDetails.getUserId(), cursor);
    }
    
}
