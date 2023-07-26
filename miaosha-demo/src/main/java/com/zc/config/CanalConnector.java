package com.zc.config;

import com.alibaba.otter.canal.client.CanalConnectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import java.net.InetSocketAddress;

/**
 * @program: MiaoSha
 * @description:
 * @author: ZC
 * @create: 2023-07-21 11:22
 **/
@Component
public class CanalConnector {
    @Value("${canal.ip}")
    private String ip;
    @Value("${canal.port}")
    private int port;
    @Value("${canal.destination}")
    private String destination;
    @Value("${canal.username}")
    private String username;
    @Value("${canal.password}")
    private String password;

    @Bean
    public com.alibaba.otter.canal.client.CanalConnector newInstance(){
        return CanalConnectors.newSingleConnector(new InetSocketAddress(ip, port),
                destination, username, password);
    }

}
