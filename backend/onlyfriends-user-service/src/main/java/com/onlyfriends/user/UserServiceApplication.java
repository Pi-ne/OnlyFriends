package com.onlyfriends.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.onlyfriends.user.mapper")
@SpringBootApplication(scanBasePackages = {"com.onlyfriends.user", "com.onlyfriends.common"})
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
