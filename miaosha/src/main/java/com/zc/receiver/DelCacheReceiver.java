package com.zc.receiver;


import com.zc.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * rabbitMq接收者
 * 为方便初学者启动项目，暂时注释掉@Component，需要使用请去除@Component的注释
 */
@Component
@RabbitListener(queues = "delCache")
public class DelCacheReceiver {

    private static final Logger LOGGER = LoggerFactory.getLogger(DelCacheReceiver.class);

    @Autowired
    private StockService stockService;

    @RabbitHandler
    public void process(String message) {
        LOGGER.info("DelCacheReceiver收到消息: " + message);
        LOGGER.info("DelCacheReceiver开始删除缓存: " + message);
        stockService.delStockCountCache(Integer.parseInt(message));
    }
}
