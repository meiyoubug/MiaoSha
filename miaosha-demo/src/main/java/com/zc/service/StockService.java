package com.zc.service;

import com.zc.entity.Stock;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 * 服务类
 * </p>
 *
 * @author admin
 * @since 2023-07-14
 */
public interface StockService extends IService<Stock> {
    void delStockCountCache(int sid);
    void setStockCountToCache(int sid, int count);

    Integer getStockCountByCache(int sid);

    int getStockCountByDB(int sid);

    int addUserCount(Integer userId) throws Exception;

    boolean getUserIsBanned(Integer userId);

    int createWrongOrder(int sid);

    int createOptimisticOrder(int sid);

    int createVerifiedOrder(Integer sid, Integer userId, String verifyHash) throws Exception;
}
