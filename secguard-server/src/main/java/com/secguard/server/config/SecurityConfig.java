package com.secguard.server.config;

import com.secguard.server.security.AgentKeyAuthFilter;
import com.secguard.server.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security 配置
 *
 * 安全策略：
 * 1. Agent API（/api/agents/register）→ 公开，无需认证
 * 2. Agent API（/api/agents/heartbeat, /api/events/*）→ X-Agent-Key 认证
 * 3. Auth API（/api/auth/*）→ 公开（登录接口）
 * 4. 管理端 API（/api/alerts, /api/rules 等）→ JWT 认证
 * 5. WebSocket（/ws/*）→ 暂不认证（W9 再加）
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AgentKeyAuthFilter agentKeyAuthFilter;
    private final JwtAuthFilter jwtAuthFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 禁用 CSRF（API 服务不需要）
            .csrf(AbstractHttpConfigurer::disable)
            // 无状态会话
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            // 请求授权规则
            .authorizeHttpRequests(auth -> auth
                // 公开接口
                .requestMatchers(HttpMethod.POST, "/api/agents/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers("/ws/**").permitAll()
                .requestMatchers("/actuator/**").permitAll()
                // Agent 通信接口（由 AgentKeyAuthFilter 处理）
                .requestMatchers(HttpMethod.POST, "/api/agents/heartbeat").hasRole("AGENT")
                .requestMatchers(HttpMethod.POST, "/api/events/**").hasRole("AGENT")
                // 管理端接口需要认证（含 GET /api/events/* 日志查询）
                .anyRequest().authenticated()
            )
            // 添加自定义过滤器（在 UsernamePasswordAuthenticationFilter 之前）
            .addFilterBefore(agentKeyAuthFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
