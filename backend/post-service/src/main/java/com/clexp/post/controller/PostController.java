package com.clexp.post.controller;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import com.clexp.post.dto.CommentRequest;
import com.clexp.post.dto.CommentResponse;
import com.clexp.post.dto.LikeResponse;
import com.clexp.post.dto.PostRequest;
import com.clexp.post.dto.PostResponse;
import com.clexp.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    // Posts

    @PostMapping("/posts")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<PostResponse> createPost(@AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody PostRequest request) {

        return postService.createPost(userDetails.getUserId(), request);
    }

    @GetMapping("/posts/{postId}")
    public Mono<PostResponse> getPostById(@AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID postId) {
        return postService.getPostById(userDetails.getUserId(), postId);
    }

    @GetMapping("/posts")
    public Flux<PostResponse> getAllPosts(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return postService.getAllPosts(userDetails.getUserId());
    }

    @GetMapping("/users/{userId}/posts")
    public Flux<PostResponse> getPostsByUserId(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID userId) {
        return postService.getPostsByUserId(userDetails.getUserId(), userId);
    }

    @PutMapping("/posts/{postId}")
    public Mono<PostResponse> updatePost(@AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID postId, @Valid @RequestBody PostRequest request) {

        return postService.updatePost(userDetails.getUserId(), postId, request);
    }

    @DeleteMapping("/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deletePost(@PathVariable UUID postId) {
        return postService.deletePost(postId);
    }

    // Comments

    @PostMapping("/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<CommentResponse> createComment(
            @AuthenticationPrincipal CustomUserDetails userDetails, @PathVariable UUID postId,
            @Valid @RequestBody CommentRequest request) {

        return postService.createComment(userDetails.getUserId(), postId, request);
    }

    @GetMapping("/posts/{postId}/comments")
    public Flux<CommentResponse> getCommentsByPostId(@PathVariable UUID postId) {
        return postService.getCommentsByPostId(postId);
    }

    @GetMapping("/comments/{commentId}")
    public Mono<CommentResponse> getCommentById(@PathVariable UUID commentId) {
        return postService.getCommentById(commentId);
    }

    @PutMapping("/comments/{commentId}")
    public Mono<CommentResponse> updateComment(@PathVariable UUID commentId,
            @Valid @RequestBody CommentRequest request) {

        return postService.updateComment(commentId, request);
    }

    @DeleteMapping("/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteComment(@PathVariable UUID commentId) {
        return postService.deleteComment(commentId);
    }

    // Likes

    @PostMapping("/posts/{postId}/likes")
    @ResponseStatus(HttpStatus.CREATED)
    public Mono<LikeResponse> createLike(@AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID postId) {

        return postService.createLike(userDetails.getUserId(), postId);
    }

    @GetMapping("/posts/{postId}/likes")
    public Flux<LikeResponse> getLikesByPostId(@PathVariable UUID postId) {
        return postService.getLikesByPostId(postId);
    }

    @GetMapping("/user/{userId}/likes")
    public Flux<LikeResponse> getLikesByUserId(@PathVariable UUID userId) {
        return postService.getLikesByUserId(userId);
    }

    @DeleteMapping("/posts/{postId}/likes")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> deleteLike(@AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable UUID postId) {

        return postService.deleteLike(userDetails.getUserId(), postId);
    }
}
