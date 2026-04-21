package com.clexp.user.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

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
import com.clexp.user.dto.LanguagePreference;
import com.clexp.user.dto.UserResponse;
import com.clexp.user.dto.UserUpdateRequest;
import com.clexp.user.model.UserInterest;
import com.clexp.user.model.UserLanguage;
import com.clexp.user.model.UserLanguageStatus;
import com.clexp.user.repository.InterestRepository;
import com.clexp.user.repository.LanguageRepository;
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

    public Mono<UserResponse> getUser(UUID userId) {
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new BusinessException("User not found", HttpStatus.NOT_FOUND)))
            .flatMap(user -> 
                Mono.zip(
                    loadUserLanguages(userId),
                    loadUserInterests(userId)
                ).map(tuple -> {
                    Map<UserLanguageStatus, Set<Language>> languagesMap = tuple.getT1();
                    Set<Interest> interests = tuple.getT2();
                    
                    return mapToFullUser(user, languagesMap, interests);
                })
            );
    }

    @Transactional
    public Mono<UserResponse> updateUser(UUID userId, UserUpdateRequest request) {
        log.info("Updating user: {}", userId);
        
        return userRepository.findById(userId)
            .switchIfEmpty(Mono.error(new BusinessException("User not found", HttpStatus.NOT_FOUND)))
            .flatMap(user -> {
                user.setLocation(request.getLocation());
                user.setBio(request.getBio());
                user.setAvatarUrl(request.getAvatarUrl());                
                user.setUpdatedAt(LocalDateTime.now());
                
                return userRepository.save(user)
                    .flatMap(savedUser -> 
                        // Обновляем языки и интересы
                        updateUserLanguages(userId, request.getLanguages())
                            .then(updateUserInterests(userId, request.getInterestIds()))
                            .then(Mono.zip(
                                loadUserLanguages(userId),
                                loadUserInterests(userId)
                            ))
                    )
                    .map(tuple -> {
                        Map<UserLanguageStatus, Set<Language>> languagesMap = tuple.getT1();
                        Set<Interest> interests = tuple.getT2();
                        return mapToFullUser(user, languagesMap, interests);
                    });
            });
    }

    private Mono<Void> updateUserLanguages(UUID userId, Set<LanguagePreference> languages) {
        if (languages == null) {
            return Mono.empty();
        }
        
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

    private Mono<Void> updateUserInterests(UUID userId, Set<UUID> interestIds) {
        if (interestIds == null) {
            return Mono.empty();
        }
        
        return userInterestRepository.deleteAllByUserId(userId)
            .thenMany(Flux.fromIterable(interestIds))
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

    private Mono<Map<UserLanguageStatus, Set<Language>>> loadUserLanguages(UUID userId) {
        return userLanguageRepository.findAllByUserId(userId)
            .flatMap(ul -> 
                languageRepository.findById(ul.getLanguageId())
                    .map(lang -> Map.entry(ul.getStatus(), lang))
            )
            .collectMultimap(Map.Entry::getKey, Map.Entry::getValue)
            .map(multimap -> {
                Map<UserLanguageStatus, Set<Language>> result = new HashMap<>();
                multimap.forEach((status, languages) -> 
                    result.put(status, new HashSet<>(languages))
                );
                return result;
            });
    }

    private Mono<Set<Interest>> loadUserInterests(UUID userId) {
        return userInterestRepository.findAllByUserId(userId)
            .flatMap(ui -> interestRepository.findById(ui.getInterestId()))
            .collect(Collectors.toSet());
    }

    private UserResponse mapToFullUser(
            User user, 
            Map<UserLanguageStatus, Set<Language>> languagesMap,
            Set<Interest> interests) {
        
        Set<LanguageDto> knownLanguages = languagesMap.getOrDefault(UserLanguageStatus.KNOWN, Collections.emptySet())
            .stream()
            .map(lang -> LanguageDto.builder()
                .id(lang.getId())
                .code(lang.getCode())
                .name(lang.getName())
                .build())
            .collect(Collectors.toSet());
        
        Set<LanguageDto> learningLanguages = languagesMap.getOrDefault(UserLanguageStatus.LEARNING, Collections.emptySet())
            .stream()
            .map(lang -> LanguageDto.builder()
                .id(lang.getId())
                .code(lang.getCode())
                .name(lang.getName())
                .build())
            .collect(Collectors.toSet());
        
        Set<InterestDto> interestDtos = interests.stream()
            .map(interest -> InterestDto.builder()
                .id(interest.getId())
                .name(interest.getName())
                .build())
            .collect(Collectors.toSet());
        
        return UserResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .fullName(user.getFullName())
            .age(user.getAge())
            .location(user.getLocation())
            .bio(user.getBio())
            .avatarUrl(user.getAvatarUrl())
            .lastLoginAt(user.getLastLoginAt())
            .knownLanguages(knownLanguages)
            .learningLanguages(learningLanguages)
            .interests(interestDtos)
            .build();
    }
}
