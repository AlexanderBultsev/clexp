package com.clexp.user.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.clexp.auth.repository.UserRepository;
import com.clexp.common.dto.InterestDto;
import com.clexp.common.dto.LanguageDto;
import com.clexp.common.exception.BusinessException;
import com.clexp.common.model.Interest;
import com.clexp.common.model.Language;
import com.clexp.common.model.User;
import com.clexp.common.repository.InterestRepository;
import com.clexp.common.repository.LanguageRepository;
import com.clexp.user.dto.LanguagePreference;
import com.clexp.user.dto.ShortUserResponse;
import com.clexp.user.dto.UserResponse;
import com.clexp.user.dto.UserUpdateRequest;
import com.clexp.user.model.Subscription;
import com.clexp.user.model.UserInterest;
import com.clexp.user.model.UserLanguage;
import com.clexp.user.model.UserLanguageStatus;
import com.clexp.user.repository.SubscriptionRepository;
import com.clexp.user.repository.UserInterestRepository;
import com.clexp.user.repository.UserLanguageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final LanguageRepository languageRepository;
    private final InterestRepository interestRepository;
    private final UserLanguageRepository userLanguageRepository;
    private final UserInterestRepository userInterestRepository;
    private final SubscriptionRepository subscriptionRepository;

    // Users

    public Mono<UserResponse> getUserById(UUID currentUserId, UUID userId) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new BusinessException("User not found", HttpStatus.NOT_FOUND)))
            .flatMap(user -> mapToUserResponse(user, currentUserId));
    }

    public Mono<ShortUserResponse> getShortUserById(UUID userId) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new BusinessException("User not found", HttpStatus.NOT_FOUND)))
            .map(this::mapToShortUserResponse);
    }

    @Transactional
    public Mono<UserResponse> updateUser(UUID userId, UserUpdateRequest request) {
        log.info("Updating user: {}", userId);

        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new BusinessException("User not found", HttpStatus.NOT_FOUND)))
            .flatMap(user -> {
                user.setFullName(request.getFullName());
                user.setAge(request.getAge());
                user.setLocation(request.getLocation());
                user.setBio(request.getBio());
                user.setRole(request.getRole());
                user.setAvatarUrl(request.getAvatarUrl());
                user.setUpdatedAt(LocalDateTime.now());

                return userRepository.save(user)
                    .flatMap(savedUser ->
                        updateUserLanguages(userId, request.getLanguages())
                            .then(updateUserInterests(userId, request.getInterests()))
                            .then(mapToUserResponse(user, userId))
                    );
            });
    }

    // Subscriptions

    /** Подписаться на пользователя. */
    @Transactional
    public Mono<Void> subscribe(UUID sourceId, UUID targetId) {
        if (sourceId.equals(targetId)) {
            return Mono.error(new BusinessException("Cannot subscribe to yourself", HttpStatus.BAD_REQUEST));
        }

        return userRepository.existsById(targetId)
            .flatMap(exists -> {
                if (!exists)
                    return Mono.error(new BusinessException("Target user not found", HttpStatus.NOT_FOUND));

                return subscriptionRepository.existsBySourceIdAndTargetId(sourceId, targetId)
                    .flatMap(alreadySubscribed -> {
                        if (alreadySubscribed)
                            return Mono.error(new BusinessException("Already subscribed", HttpStatus.CONFLICT));

                        Subscription subscription = Subscription.builder()
                            .sourceId(sourceId)
                            .targetId(targetId)
                            .createdAt(LocalDate.now())
                            .build();

                        return subscriptionRepository.save(subscription).then();
                    });
            });
    }

    /** Отписаться от пользователя. */
    @Transactional
    public Mono<Void> unsubscribe(UUID sourceId, UUID targetId) {
        return subscriptionRepository.existsBySourceIdAndTargetId(sourceId, targetId)
            .flatMap(exists -> {
                if (!exists)
                    return Mono.error(new BusinessException("Subscription not found", HttpStatus.NOT_FOUND));

                return subscriptionRepository.deleteBySourceIdAndTargetId(sourceId, targetId);
            });
    }

    // Utils

    public Flux<LanguageDto> getLanguages() {
        return languageRepository.findAll()
            .map(this::mapToLanguageDto);
    }

    public Flux<InterestDto> getInterests() {
        return interestRepository.findAll()
            .map(this::mapToInterestDto);
    }

    private Mono<Void> updateUserLanguages(UUID userId, List<LanguagePreference> languages) {
        if (languages == null) return Mono.empty();
        
        return userLanguageRepository.deleteAllByUserId(userId)
            .thenMany(Flux.fromIterable(languages))
            .flatMap(lang -> 
                userLanguageRepository.save(
                    UserLanguage.builder()
                        .userId(userId)
                        .languageId(lang.getLanguageId())
                        .status(lang.getStatus())
                        .build()
                )
            )
            .then();
    }

    private Mono<Void> updateUserInterests(UUID userId, List<UUID> interests) {
        if (interests == null) return Mono.empty();
        
        return userInterestRepository.deleteAllByUserId(userId)
            .thenMany(Flux.fromIterable(interests))
            .flatMap(interestId -> 
                userInterestRepository.save(
                    UserInterest.builder()
                        .userId(userId)
                        .interestId(interestId)
                        .build()
                )
            )
            .then();
    }

    public Mono<UserResponse> mapToUserResponse(User user, UUID currentUserId) {
        return Mono.zip(
            getUserLanguagesByStatus(user.getId(), UserLanguageStatus.KNOWN),
            getUserLanguagesByStatus(user.getId(), UserLanguageStatus.LEARNING),
            getUserInterests(user.getId()),
            getIsSubscribed(currentUserId, user.getId())
        ).map(tuple -> UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .age(user.getAge())
                .location(user.getLocation())
                .bio(user.getBio())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .lastLoginAt(user.getLastLoginAt())
                .knownLanguages(tuple.getT1())
                .learningLanguages(tuple.getT2())
                .interests(tuple.getT3())
                .isSubscribed(tuple.getT4())
                .build()
        );
    }

    private ShortUserResponse mapToShortUserResponse(User user) {
        return ShortUserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .avatarUrl(user.getAvatarUrl())
            .role(user.getRole())
            .build();
    }

    /** Флаг подписки. Возвращает null если currentUserId == null. */
    private Mono<Boolean> getIsSubscribed(UUID currentUserId, UUID targetUserId) {
        if (currentUserId == null || currentUserId.equals(targetUserId))
            return Mono.just(false);

        return subscriptionRepository
            .existsBySourceIdAndTargetId(currentUserId, targetUserId)
            .defaultIfEmpty(false);
    }

    private Mono<List<LanguageDto>> getUserLanguagesByStatus(UUID userId, UserLanguageStatus languageStatus) {
        return userLanguageRepository.findAllByUserIdAndStatus(userId, languageStatus)
            .flatMap(userLanguage -> languageRepository.findById(userLanguage.getLanguageId()))
            .map(this::mapToLanguageDto)
            .collectList();
    }

    private Mono<List<InterestDto>> getUserInterests(UUID userId) {
        return userInterestRepository.findAllByUserId(userId)
            .flatMap(ui -> interestRepository.findById(ui.getInterestId()))
            .map(this::mapToInterestDto)
            .collectList();
    }

    public LanguageDto mapToLanguageDto(Language language) {
        return LanguageDto.builder()
            .id(language.getId())
            .code(language.getCode())
            .name(language.getName())
            .build();
    }

    private InterestDto mapToInterestDto(Interest interest) {
        return InterestDto.builder()
            .id(interest.getId())
            .name(interest.getName())
            .build();
    }
}
