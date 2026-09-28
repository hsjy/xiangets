package com.yuetin.music;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.yuetin.music.mapper")
public class YuetinApplication {
    public static void main(String[] args) {
        SpringApplication.run(YuetinApplication.class, args);
    }
}
