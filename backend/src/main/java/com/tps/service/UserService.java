package com.tps.service;

import com.tps.dto.user.UpdateProfileRequest;
import com.tps.dto.user.UserProfileResponse;
import com.tps.entity.User;
import com.tps.repository.ProductRepository;
import com.tps.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final FileService fileService;

    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        return toResponse(user);
    }

    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (req.getNickname() != null) user.setNickname(req.getNickname());
        if (req.getBio() != null) user.setBio(req.getBio());
        if (req.getLocation() != null) user.setLocation(req.getLocation());
        if (req.getShippingAddress() != null) user.setShippingAddress(req.getShippingAddress());
        if (req.getAvatarUrl() != null) user.setAvatarUrl(req.getAvatarUrl());
        userRepository.save(user);
        return toResponse(user);
    }

    public UserProfileResponse updateAvatar(Long userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (avatarUrl == null || avatarUrl.isBlank()) {
            throw new IllegalArgumentException("头像地址不能为空");
        }
        user.setAvatarUrl(avatarUrl);
        userRepository.save(user);
        return toResponse(user);
    }

    public void deactivate(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        user.setStatus(User.UserStatus.DEACTIVATED);
        userRepository.save(user);
    }

    private UserProfileResponse toResponse(User user) {
        UserProfileResponse r = new UserProfileResponse();
        r.setId(user.getId());
        r.setPhone(user.getPhone());
        r.setStudentId(user.getStudentId());
        r.setNickname(user.getNickname());
        r.setAvatarUrl(fileService.toAbsoluteUrl(user.getAvatarUrl()));
        r.setBio(user.getBio());
        r.setLocation(user.getLocation());
        r.setShippingAddress(user.getShippingAddress());
        r.setCreditScore(user.getCreditScore());
        r.setRole(user.getRole().name());
        r.setStatus(user.getStatus().name());
        r.setProductCount(productRepository.findByUserId(user.getId()).size());
        return r;
    }
}
