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
     int addUserCount(Integer userId) throws Exception;

     boolean getUserIsBanned(Integer userId);

     int createWrongOrder(int sid);

     int createOptimisticOrder(int sid);

     int createVerifiedOrder(Integer sid,Integer userId,String verifyHash) throws Exception;
}
