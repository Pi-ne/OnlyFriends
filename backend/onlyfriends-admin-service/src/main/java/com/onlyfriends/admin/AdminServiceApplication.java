package com.onlyfriends.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.onlyfriends.admin.config.AdminFeignConfig;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients(defaultConfiguration = AdminFeignConfig.class)
@MapperScan("com.onlyfriends.admin.mapper")
@SpringBootApplication(scanBasePackages = {"com.onlyfriends.admin", "com.onlyfriends.common"})
public class AdminServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
