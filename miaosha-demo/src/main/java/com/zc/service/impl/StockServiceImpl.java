package com.zc.service.impl;

import com.zc.entity.Stock;
import com.zc.entity.StockOrder;
import com.zc.mapper.StockMapper;
import com.zc.mapper.StockOrderMapper;
import com.zc.service.StockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author admin
 * @since 2023-07-14
 */
@Service
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockServiceImpl.class);
    @Resource
    private StockMapper stockMapper;
    @Resource
    private StockOrderMapper stockOrderMapper;
    @Override
    public int createWrongOrder(int sid) {
        Stock stock=checkStock(sid);
        saleStock(sid);
        checkStock(sid);
        int id=createOrder(stock);
        return id;
    }

    @Override
    public int createOptimisticOrder(int sid) {
        //校验库存
        Stock stock=checkStock(sid);
        //乐观锁更新库存
        saleStockOptimistic(stock);
        //创建订单
        int id=createOrder(stock);
        return stock.getCount()-(stock.getSale()+1);
    }

    private void saleStockOptimistic(Stock stock){
        LOGGER.info("查询数据库尝试更新库存");
        int count=stockMapper.updateByOptimistic(stock);
        if(count==0){
            throw new RuntimeException("并发更新库存失败，version不匹配");
        }
    }


    private Stock checkStock(int sid){
        Stock stock=stockMapper.selectById(sid);
        if(stock.getSale().equals(stock.getCount()+1)){
            throw new RuntimeException("库存不足");
        }
        return stock;
    }


    private void saleStock(int sid){
        stockMapper.updateSaleCnt(sid);
    }

    private int createOrder(Stock stock){
        StockOrder stockOrder=new StockOrder();
        stockOrder.setSid(stock.getId());
        stockOrder.setName(stock.getName());
        int id= stockOrderMapper.insert(stockOrder);
        return id;
    }
}