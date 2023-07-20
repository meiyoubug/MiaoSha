package com.zc.config;


import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @program: MiaoSha
 * @description:
 * @author: ZC
 * @create: 2023-07-20 15:38
 **/
@Configuration
public class RabbitMqConfig {
    @Bean
    public Queue delCacheQueue(){
        return new Queue("delCache");
    }
}
