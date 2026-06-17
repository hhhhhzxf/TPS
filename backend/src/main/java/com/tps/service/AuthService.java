package com.tps.service;

/**
 * 文件说明：业务服务层，负责封装核心业务规则、事务与对象组装。
 */

import com.tps.dto.auth.*;
import com.tps.entity.User;
import com.tps.repository.UserRepository;
import com.tps.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    private static final String MOCK_CODE = "1234";
    private static final Map<String, String> ADMIN_LOGIN_PHONES = Map.of(
            "admin", "18888888888",
            "admin1", "18888888881",
            "admin2", "18888888882"
    );

    public void sendCode(String phone) {
        // 开发阶段固定验证码1234，不做实际发送
    }

    public LoginResponse register(RegisterRequest req) {
        if (!MOCK_CODE.equals(req.getCode())) {
            throw new IllegalArgumentException("验证码错误");
        }
        if (userRepository.existsByPhone(req.getPhone())) {
            throw new IllegalArgumentException("手机号已注册");
        }
        if (userRepository.existsByStudentId(req.getStudentId())) {
            throw new IllegalArgumentException("学号已认证");
        }
        User user = new User();
        user.setPhone(req.getPhone());
        user.setStudentId(req.getStudentId());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname() != null ? req.getNickname() : "用户" + req.getPhone().substring(7));
        userRepository.save(user);
        return buildLoginResponse(user);
    }

    public LoginResponse login(LoginRequest req) {
        String loginName = req.getPhone();
        String phone = ADMIN_LOGIN_PHONES.getOrDefault(loginName.toLowerCase(), loginName);
        User user = userRepository.findByPhone(phone)
                .or(() -> userRepository.findByStudentId(loginName))
                .orElseThrow(() -> new IllegalArgumentException("账号未注册"));
        if (user.getRole() == User.Role.ADMIN && user.getStatus() != User.UserStatus.ACTIVE) {
            user.setStatus(User.UserStatus.ACTIVE);
            userRepository.save(user);
        }
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new IllegalArgumentException("账号被封禁，请联系管理员");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("密码错误");
        }
        return buildLoginResponse(user);
    }

    public LoginResponse refresh(String refreshToken) {
        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("刷新令牌无效");
        }
        Long userId = jwtUtil.getUserId(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new IllegalArgumentException("账号被封禁，请联系管理员");
        }
        return buildLoginResponse(user);
    }

    private LoginResponse buildLoginResponse(User user) {
        String token = jwtUtil.generateToken(user.getId(), user.getRole().name());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getRole().name());
        return new LoginResponse(token, refreshToken, user.getId(), user.getNickname(), user.getAvatarUrl(), user.getRole().name());
    }
}
