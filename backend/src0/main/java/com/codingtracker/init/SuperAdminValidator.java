package com.codingtracker.init;

import com.codingtracker.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 系统启动时验证超级管理员唯一性
 */
@Component
public class SuperAdminValidator implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminValidator.class);

    @Autowired
    private UserService userService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        try {
            long superAdminCount = userService.getSuperAdminCount();

            if (superAdminCount == 0) {
                logger.warn("系统中没有超级管理员，请手动添加一个超级管理员用户");
            } else if (superAdminCount == 1) {
                logger.info("系统中存在 1 个超级管理员，符合安全要求");
            } else {
                logger.error("警告：系统中存在 {} 个超级管理员，违反了唯一性要求！", superAdminCount);
                logger.error("请检查数据库并确保只有一个超级管理员用户");
                // 这里可以选择抛出异常来阻止系统启动
                // throw new RuntimeException("系统存在多个超级管理员，违反安全要求");
            }

        } catch (Exception e) {
            logger.error("验证超级管理员唯一性时发生错误: {}", e.getMessage(), e);
        }
    }
}