package com.ququ.im;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@MapperScan("com.ququ.im.mapper")
@SpringBootApplication(scanBasePackages = {"com.ququ.im", "com.ququ.common"})
public class ImServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ImServiceApplication.class, args);
    }
}
