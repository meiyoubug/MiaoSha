package com.zc.service;

import com.zc.entity.Stock;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author admin
 * @since 2023-07-14
 */
public interface StockService extends IService<Stock> {
     int createWrongOrder(int sid);

     int createOptimisticOrder(int sid);
}
