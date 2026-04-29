package com.clexp.post.service;

import com.clexp.post.repository.PostInterestRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.clexp.common.dto.InterestDto;
import com.clexp.common.exception.BusinessException;
import com.clexp.common.model.Interest;
import com.clexp.common.repository.InterestRepository;
import com.clexp.post.dto.CommentRequest;
import com.clexp.post.dto.CommentResponse;
import com.clexp.post.dto.LikeResponse;
import com.clexp.post.dto.PostRequest;
import com.clexp.post.dto.PostResponse;
import com.clexp.post.model.Comment;
import com.clexp.post.model.Like;
import com.clexp.post.model.Post;
import com.clexp.post.model.PostInterest;
import com.clexp.post.repository.CommentRepository;
import com.clexp.post.repository.LikeRepository;
import com.clexp.post.repository.PostRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final LikeRepository likeRepository;
    private final PostInterestRepository postInterestRepository;
    private final InterestRepository interestRepository;

    // Posts

    public Mono<PostResponse> createPost(UUID currentUserId, PostRequest request) {
        Post post = Post.builder()
            .userId(currentUserId)
            .content(request.getContent())
            .mediaUrls(request.getMediaUrls())
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
        
        return postRepository.save(post)
            .flatMap(savedPost -> savePostInterests(savedPost, request.getInterests()))
            .flatMap(savedPost -> mapToPostResponse(savedPost, currentUserId));
    }

    public Mono<PostResponse> getPostById(UUID currentUserId, UUID postId) {
        return postRepository.findById(postId)
            .switchIfEmpty(Mono.error(new BusinessException("Post not found", HttpStatus.NOT_FOUND)))
            .flatMap(post -> mapToPostResponse(post, currentUserId));
    }

    public Flux<PostResponse> getAllPosts(UUID currentUserId) {
        return postRepository.findAll()
            .flatMap(post -> mapToPostResponse(post, currentUserId));
    }

    public Flux<PostResponse> getPostsByUserId(UUID currentUserId, UUID userId) {
        return postRepository.findAllByUserId(userId)
            .flatMap(post -> mapToPostResponse(post, currentUserId));
    }

    @Transactional
    public Mono<PostResponse> updatePost(UUID currentUserId, UUID postId, PostRequest request) {
        return postRepository.findById(postId)
                .switchIfEmpty(Mono.error(new BusinessException("Post not found", HttpStatus.NOT_FOUND)))
                .flatMap(existingPost -> {
                    existingPost.setContent(request.getContent());
                    existingPost.setMediaUrls(request.getMediaUrls());
                    existingPost.setUpdatedAt(LocalDateTime.now());
                    return postInterestRepository.deleteAllByPostId(postId)
                        .then(savePostInterests(existingPost, request.getInterests()))
                        .then(postRepository.save(existingPost));
                })
                .flatMap(post -> mapToPostResponse(post, currentUserId));
    }

    @Transactional
    public Mono<Void> deletePost(UUID postId) {
        return postRepository.deleteById(postId);
    }

    // Comments

    public Mono<CommentResponse> createComment(UUID currentUserId, UUID postId, CommentRequest request) {
        return postRepository.existsById(postId)
            .flatMap(postExists -> {
                
                if (!postExists) return Mono.error(new BusinessException("Post not found", HttpStatus.NOT_FOUND));

                Comment comment = Comment.builder()
                    .postId(postId)
                    .userId(currentUserId)
                    .content(request.getContent())
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
                
                return commentRepository.save(comment)
                    .map(this::mapToCommentResponse);
            });
    }

    public Mono<CommentResponse> getCommentById(UUID commentId) {
        return commentRepository.findById(commentId)
            .switchIfEmpty(Mono.error(new BusinessException("Comment not found", HttpStatus.NOT_FOUND)))
            .map(this::mapToCommentResponse);
    }

    public Flux<CommentResponse> getCommentsByPostId(UUID postId) {
        return commentRepository.findAllByPostId(postId)
                .map(this::mapToCommentResponse);
    }

    public Flux<CommentResponse> getCommentsByUserId(UUID userId) {
        return commentRepository.findAllByUserId(userId)
                .map(this::mapToCommentResponse);
    }

    @Transactional
    public Mono<CommentResponse> updateComment(UUID commentId, CommentRequest request) {
        return commentRepository.findById(commentId)
                .switchIfEmpty(Mono.error(new BusinessException("Comment not found", HttpStatus.NOT_FOUND)))
                .flatMap(existingComment -> {
                    if (request.getContent() != null) existingComment.setContent(request.getContent());
                    existingComment.setUpdatedAt(LocalDateTime.now());
                    return commentRepository.save(existingComment);
                })
                .map(this::mapToCommentResponse);
    }

    @Transactional
    public Mono<Void> deleteComment(UUID commentId) {
        return commentRepository.deleteById(commentId);
    }

    // Likes

    public Mono<LikeResponse> createLike(UUID currentUserId, UUID postId) {
        return postRepository.existsById(postId)
            .flatMap(postExists -> {
                if (!postExists) return Mono.error(new BusinessException("Post not found", HttpStatus.NOT_FOUND));
                return likeRepository.existsByUserIdAndPostId(currentUserId, postId)
                    .flatMap(likeExists -> {
                        if (likeExists) return Mono.error(new BusinessException("Already liked this post", HttpStatus.CONFLICT));
                    
                        Like like = Like.builder()
                            .userId(currentUserId)
                            .postId(postId)
                            .createdAt(LocalDateTime.now())
                            .build();
                
                        return likeRepository.save(like)
                            .map(this::mapToLikeResponse);
                    });
            });
    }

    public Flux<LikeResponse> getLikesByPostId(UUID postId) {
        return likeRepository.findAllByPostId(postId)
                .map(this::mapToLikeResponse);
    }

    public Flux<LikeResponse> getLikesByUserId(UUID userId) {
        return likeRepository.findAllByUserId(userId)
                .map(this::mapToLikeResponse);
    }

    public Mono<Boolean> getIsLikedByUser(UUID currentUserId, UUID postId) {
        if (currentUserId == null) return Mono.just(false);

        return likeRepository.existsByUserIdAndPostId(currentUserId, postId)
            .defaultIfEmpty(false);
    }

    @Transactional
    public Mono<Void> deleteLike(UUID currentUserId, UUID postId) {
        return likeRepository.deleteByUserIdAndPostId(currentUserId, postId);
    }

    // Utils

    private Mono<Post> savePostInterests(Post post, List<UUID> interestsIds) {
        if (interestsIds == null || interestsIds.isEmpty())
            return Mono.just(post);

        UUID postId = post.getId();

        return Flux.fromIterable(interestsIds)
            .flatMap(interestsId -> 
                postInterestRepository.save(
                    PostInterest.builder()
                        .postId(postId)
                        .interestId(interestsId)
                        .build()
                ))
            .then(Mono.just(post));
    }

    private Mono<List<InterestDto>> getPostInterests(UUID postId) {
        return postInterestRepository.findAllByPostId(postId)
            .flatMap(postInterest -> interestRepository.findById(postInterest.getInterestId()))
            .map(this::mapToInterestDto)
            .collectList();
    }

    private InterestDto mapToInterestDto(Interest interest) {
        return InterestDto.builder()
            .id(interest.getId())
            .name(interest.getName())
            .build();
    }

    public Mono<PostResponse> mapToPostResponse(Post post, UUID currentUserId) {
        return Mono.zip(
            getCommentsByPostId(post.getId()).count(),
            getLikesByPostId(post.getId()).count(),
            getIsLikedByUser(currentUserId, post.getId()),
            getPostInterests(post.getId())
        ).map(tuple ->  PostResponse.builder()
                .id(post.getId())
                .userId(post.getUserId())
                .content(post.getContent())
                .mediaUrls(post.getMediaUrls())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .commentsCount(tuple.getT1())
                .likesCount(tuple.getT2())
                .isLiked(tuple.getT3())
                .interests(tuple.getT4())
                .build()
            );
    }

    private CommentResponse mapToCommentResponse(Comment comment) {
        return CommentResponse.builder()
            .id(comment.getId())
            .postId(comment.getPostId())
            .userId(comment.getUserId())
            .content(comment.getContent())
            .createdAt(comment.getCreatedAt())
            .updatedAt(comment.getUpdatedAt())
            .build();
    }

    private LikeResponse mapToLikeResponse(Like like) {
        return LikeResponse.builder()
            .id(like.getId())
            .userId(like.getUserId())
            .postId(like.getPostId())
            .createdAt(like.getCreatedAt())
            .build();
    }
}
