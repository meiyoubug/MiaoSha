package com.zc.controller;

import com.zc.service.StockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

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

    @RequestMapping("/createWrongOrder/{sid}")
    @ResponseBody
    public String createWrongOrder(@PathVariable int sid){
        LOGGER.info("购买物品编号sid=[{}]",sid);
        int id=0;
        try {
            id=stockService.createWrongOrder(sid);
            LOGGER.info("创建订单id：[{}]",id);
        }catch (Exception e){
            LOGGER.error("Exception",e);
        }
        return String.valueOf(id);
    }

    @RequestMapping("/createOptimisticOrder/{sid}")
    @ResponseBody
    public String createOptimisticOrder(@PathVariable int sid){
        int id=0;
        try{
            id=stockService.createOptimisticOrder(sid);
            LOGGER.info("购买成功，剩余库存为：[{}]",id);
        }catch (Exception e){
            LOGGER.error("购买失败，库存不足");
        }
        return "购买成功，剩余库存为"+id;
    }


}
