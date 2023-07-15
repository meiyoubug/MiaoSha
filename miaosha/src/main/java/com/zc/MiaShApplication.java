package com.zc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @program: Default (Template) Project
 * @description: ${description}
 * @author: ZC
 * @create: 2023-07-14 15:42
 **/
@SpringBootApplication
@MapperScan("com.zc.mapper")
public class MiaShApplication {
    public static void main(String[] args) {
        SpringApplication.run(MiaShApplication.class,args);
    }
}
