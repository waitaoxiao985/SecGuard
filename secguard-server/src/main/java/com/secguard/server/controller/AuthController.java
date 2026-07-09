package com.secguard.server.controller;

import com.secguard.common.dto.ApiResponse;
import com.secguard.server.security.JwtUtil;
import com.secguard.server.entity.SysUser;
import com.secguard.server.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 认证 API
 *
 * POST /api/auth/login - 用户登录，返回 JWT Token
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @PostMapping("/login")
    public ApiResponse<Map<String, String>> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            return ApiResponse.error(400, "用户名和密码不能为空");
        }

        SysUser user = userRepository.findByUsername(username)
                .orElse(null);

        if (user == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            return ApiResponse.error(401, "用户名或密码错误");
        }

        if (!user.getEnabled()) {
            return ApiResponse.error(403, "账号已被禁用");
        }

        // 更新最后登录时间
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        // 生成 JWT
        String token = jwtUtil.generateToken(user.getUsername(), user.getRole());

        return ApiResponse.ok(Map.of(
                "token", token,
                "role", user.getRole(),
                "displayName", user.getDisplayName() != null ? user.getDisplayName() : user.getUsername()
        ));
    }
}
