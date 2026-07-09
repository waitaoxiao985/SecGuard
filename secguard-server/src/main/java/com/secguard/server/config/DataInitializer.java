package com.secguard.server.config;

import com.secguard.server.entity.SysUser;
import com.secguard.server.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 启动时数据初始化
 * 仅在数据库中没有 admin 用户时插入默认管理员
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final SysUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            SysUser admin = SysUser.builder()
                    .username("admin")
                    .passwordHash(passwordEncoder.encode("SecGuard@2026"))
                    .role("admin")
                    .displayName("系统管理员")
                    .email("admin@secguard.local")
                    .enabled(true)
                    .build();
            userRepository.save(admin);
            log.info("✓ 默认管理员账号已创建: admin / SecGuard@2026");
        }
    }
}
