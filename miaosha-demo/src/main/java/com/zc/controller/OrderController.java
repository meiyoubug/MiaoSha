package com.zc.controller;

import com.alibaba.google.common.util.concurrent.RateLimiter;

import com.zc.service.StockService;
import com.zc.threadPool.ThreadPool;
import com.zc.utils.DelCacheByThread;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;


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
    @Resource
    private ThreadPool threadPool;


    /**
     * 每秒放行10个请求
     */
    RateLimiter rateLimiter = RateLimiter.create(10);

    /**
     * 下单接口：先删除缓存，再更新数据库，缓存延时双删
     *
     * @param sid sid
     * @return {@link String}
     */
    @RequestMapping("/createOrderWithCacheV3/{sid}")
    @ResponseBody
    public String createOrderWithCacheV3(@PathVariable int sid) {
        int count;
        try {
            // 删除库存缓存
            stockService.delStockCountCache(sid);
            // 完成扣库存下单事务
            count = stockService.createWrongOrder(sid);
            // 延时指定时间后再次删除缓存
            threadPool.execut(new DelCacheByThread(sid));
        } catch (Exception e) {
            LOGGER.error("购买失败：[{}]", e.getMessage());
            return "购买失败，库存不足";
        }
        LOGGER.info("购买成功，剩余库存为: [{}]", count);
        return String.format("购买成功，剩余库存为：%d", count);
    }



    /**
     * 下单接口：先更新数据库，再删缓存
     *
     * @param sid sid
     * @return {@link String}
     */
    @RequestMapping("/createOrderWithCacheV2/{sid}")
    @ResponseBody
    public String createOrderWithCacheV2(@PathVariable int sid) {
        int count = 0;
        try {

            //完成扣库存下单事务
            stockService.createWrongOrder(sid);

            //删除缓存
            stockService.delStockCountCache(sid);
        } catch (Exception e) {
            LOGGER.info("购买失败：[{}]", e.getMessage());
            return "购买失败";
        }
        LOGGER.info("购买成功，剩余库存为: [{}]", count);
        return String.format("购买成功，剩余库存为：%d", count);
    }


    /**
     * 下单接口：先删除缓存，再更新数据库
     *
     * @param sid sid
     * @return {@link String}
     */
    @RequestMapping("/createOrderWithCacheV1/{sid}")
    @ResponseBody
    public String createOrderWithCacheV1(@PathVariable int sid) {
        int count = 0;
        try {
            //删除缓存
            stockService.delStockCountCache(sid);
            //完成扣库存下单事务
            stockService.createWrongOrder(sid);
        } catch (Exception e) {
            LOGGER.info("购买失败：[{}]", e.getMessage());
            return "购买失败";
        }
        LOGGER.info("购买成功，剩余库存为: [{}]", count);
        return String.format("购买成功，剩余库存为：%d", count);
    }


    /**
     * 查询库存：通过数据库查询库存
     *
     * @param sid sid
     * @return {@link String}
     */
    @RequestMapping("/getStockByDB/{sid}")
    @ResponseBody
    public String getStockByDB(@PathVariable int sid) {
        int count;
        try {
            count = stockService.getStockCountByDB(sid);
        } catch (Exception e) {
            LOGGER.error("查询库存失败：[{}]", e.getMessage());
            return "查询库存失败";
        }
        LOGGER.info("商品id：[{}] 剩余库存为：[{}]", sid, count);
        return String.format("商品Id: %d 剩余库存为：%d", sid, count);
    }

    /**
     * 查询库存：通过缓存查询库存
     * 缓存命中：返回库存
     * 缓存未命中：查询数据库写入缓存并返回
     *
     * @param sid sid
     * @return {@link String}
     */
    @RequestMapping("/getStockByCache/{sid}")
    @ResponseBody
    public String getStockByCache(@PathVariable int sid) {
        Integer count;
        try {
            count = stockService.getStockCountByCache(sid);
            if (count == null) {
                count = stockService.getStockCountByDB(sid);
                LOGGER.info("缓存未命中，查询数据库，并写入缓存");
                stockService.setStockCountToCache(sid, count);
            }
        } catch (Exception e) {
            LOGGER.error("查询库存失败：[{}]", e.getMessage());
            return "查询库存失败";
        }
        LOGGER.info("商品Id: [{}] 剩余库存为: [{}]", sid, count);
        return String.format("商品Id: %d 剩余库存为：%d", sid, count);
    }


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
