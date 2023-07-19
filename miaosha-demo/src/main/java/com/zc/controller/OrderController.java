package com.zc.controller;

import com.alibaba.google.common.util.concurrent.RateLimiter;
import com.sun.org.apache.bcel.internal.generic.LOOKUPSWITCH;
import com.zc.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @program: miaosha-demo
 * @description:
 * @author: ZC
 * @create: 2023-07-14 18:16
 **/
@RestController
@RequestMapping("/order")
public class OrderController {
    private static final Logger LOGGER = LoggerFactory.getLogger(OrderController.class);

    @Resource
    private StockService stockService;


    /**
     * 每秒放行10个请求
     */
    RateLimiter rateLimiter = RateLimiter.create(10);

    @RequestMapping("/createWrongOrder/{sid}")
    @ResponseBody
    public String createWrongOrder(@PathVariable int sid) {
        LOGGER.info("购买物品编号sid=[{}]", sid);
        int id = 0;
        try {
            id = stockService.createWrongOrder(sid);
            LOGGER.info("创建订单id：[{}]", id);
        } catch (Exception e) {
            LOGGER.error("Exception", e);
        }
        return String.valueOf(id);
    }

    /**
     * 乐观锁更新库存+令牌桶限流
     *
     * @param sid sid
     * @return {@link String}
     */
    @RequestMapping("/createOptimisticOrder/{sid}")
    @ResponseBody
    public String createOptimisticOrder(@PathVariable int sid) {
        //阻塞式获取令牌
        LOGGER.info("等待时间" + rateLimiter.acquire());

//        非阻塞式获取令牌
//        if (!rateLimiter.tryAcquire(1000, TimeUnit.MILLISECONDS)) {
//            LOGGER.warn("你被限流了，真不幸，直接返回失败");
//            return "购买失败，库存不足";
//        }


        int id = 0;
        try {
            id = stockService.createOptimisticOrder(sid);
            LOGGER.info("购买成功，剩余库存为：[{}]", id);
        } catch (Exception e) {
            LOGGER.error("购买失败，库存不足");
        }
        return "购买成功";
    }

    @GetMapping("/createOrderWithVerifiedUrl/{sid}/{userId}/{verifyHash}")
    @ResponseBody
    public String createOrderWithVerifiedUrl(@PathVariable("sid") Integer sid,
                                             @PathVariable("userId") Integer userId,
                                             @PathVariable("verifyHash") String verifyHash) {
        int stockLeft;
        try {
            stockLeft = stockService.createVerifiedOrder(sid, userId, verifyHash);
            LOGGER.info("购买成功，剩余库存为：[{}]", stockLeft);
        } catch (Exception e) {
            LOGGER.error("购买失败：[{}]", e.getMessage());
            return e.getMessage();
        }
        return "购买成功，剩余库存为：" + stockLeft;
    }

    @GetMapping("/createOrderWithVerifiedUrlAndLimit/{sid}/{userId}/{verifyHash}")
    @ResponseBody
    public String createOrderWithVerifiedUrlAndLimit(@PathVariable("sid") Integer sid,
                                                     @PathVariable("userId") Integer userId,
                                                     @PathVariable("verifyHash") String verifyHash) {
        int stockLeft;
        try {
            int count = stockService.addUserCount(userId);
            LOGGER.info("用户截至该次的访问次数为: [{}]", count);
            boolean isBanned = stockService.getUserIsBanned(userId);
            if (isBanned) {
                return "购买失败，超过频率限制";
            }
            stockLeft = stockService.createVerifiedOrder(sid, userId, verifyHash);
            LOGGER.info("购买成功，剩余库存为：[{}]", stockLeft);
        } catch (Exception e) {
            LOGGER.error("购买失败：[{}]", e.getMessage());
            return e.getMessage();
        }
        return "购买成功，剩余库存为：" + stockLeft;
    }


}
