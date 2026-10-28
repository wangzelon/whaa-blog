package cn.whaa.blog.api.config;

import cn.whaa.blog.infrastructure.persistence.entity.SysUserEntity;
import cn.whaa.blog.infrastructure.persistence.mapper.SysUserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminPasswordInitializer implements ApplicationRunner {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;

    @Value("${whaa.security.default-admin-password}")
    private String defaultPassword;

    @Override
    public void run(ApplicationArguments args) {
        if (defaultPassword == null || defaultPassword.length() < 12) {
            throw new IllegalStateException("WHA_ADMIN_PASSWORD must contain at least 12 characters.");
        }
        SysUserEntity admin = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>().eq(SysUserEntity::getUsername, "admin"));
        if (admin == null) {
            admin = new SysUserEntity();
            admin.setUsername("admin");
            admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
            admin.setNickname("Whaa");
            admin.setRole("ADMIN");
            sysUserMapper.insert(admin);
            log.info("Default admin account initialized; password is configured via WHA_ADMIN_PASSWORD.");
            return;
        }
        if (!passwordEncoder.matches(defaultPassword, admin.getPasswordHash())) {
            admin.setPasswordHash(passwordEncoder.encode(defaultPassword));
            sysUserMapper.updateById(admin);
            log.info("Default admin password initialized (username: admin). See docs for dev password.");
        }
    }
}
