package com.zc;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @program: miaosha-demo
 * @description:
 * @author: ZC
 * @create: 2023-07-14 18:30
 **/
@SpringBootApplication
@MapperScan("com.zc.mapper")
public class StartApplication {
    public static void main(String[] args) {
        SpringApplication.run(StartApplication.class,args);
    }
}
