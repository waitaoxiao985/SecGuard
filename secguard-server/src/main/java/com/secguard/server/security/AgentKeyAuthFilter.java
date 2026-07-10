package com.secguard.server.security;

import com.secguard.server.service.AgentService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Agent 认证过滤器
 *
 * 拦截 Agent API 请求，验证 X-Agent-Key 请求头。
 * 只对 /api/agents/heartbeat 等需要 Agent 身份的端点生效。
 * /api/agents/register 在 SecurityConfig 中已放行。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AgentKeyAuthFilter extends OncePerRequestFilter {

    private final AgentService agentService;
    private static final String AGENT_KEY_HEADER = "X-Agent-Key";

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        // 只拦截 POST 请求到 Agent 专属端点（GET 走 JWT 认证）
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/agents/heartbeat")) {
            return false;
        }
        if ("POST".equalsIgnoreCase(method) && path.startsWith("/api/events/")) {
            return false;
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String agentKey = request.getHeader(AGENT_KEY_HEADER);

        if (agentKey == null || agentKey.isBlank()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"message\":\"Missing X-Agent-Key header\"}");
            return;
        }

        if (!agentService.validateAgentKey(agentKey)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"code\":401,\"message\":\"Invalid or disabled agent key\"}");
            return;
        }

        // 设置 Agent 身份到 SecurityContext
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "agent:" + agentKey, null,
                List.of(new SimpleGrantedAuthority("ROLE_AGENT"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        filterChain.doFilter(request, response);
    }
}
