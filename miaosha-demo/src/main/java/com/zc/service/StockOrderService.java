package com.zc.service;

import com.zc.entity.StockOrder;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author admin
 * @since 2023-07-14
 */
public interface StockOrderService extends IService<StockOrder> {
    /**
     * 创建订单,sid
     *
     * @param userId 用户id
     * @param sid    sid
     * @return {@link String}
     */
    String createOrderBySid(int userId,int sid) throws Exception;


    /**
     * 生成key
     *
     * @param sid sid
     * @return {@link String}
     */
    String returnGoodsHashKey(int sid);

    /**
     * 检查库存
     *
     * @param sid sid
     * @return boolean
     */
    boolean checkCount(int sid);

    /**
     * 验证用户是否有效
     *
     * @param userId 用户id
     * @return boolean
     */
    boolean verifyUser(int userId);

    /**
     * 向缓存中添加库存
     *
     * @param sid   sid
     * @param count 数
     */
    void setCountToCache(int sid,int count);

    /**
     * 从缓存中获取库存
     *
     * @param sid sid
     * @return int
     */
    int getCountFromCache(int sid);

    /**
     * 从缓存中删除库存
     *
     * @param sid sid
     */
    void deleteCountFromCache(int sid);


    /**
     * 从数据库获取库存
     *
     * @param sid sid
     * @return int
     */
    int getCountFromDB(int sid);


    /**
     * 销售商品
     *
     * @param sid sid
     * @return int
     */
    int saleGoods(int sid);

    /**
     * 创建订单
     *
     * @param sid    sid
     * @param userId 用户id
     * @return int
     */
    int createOrder(int sid,int userId);
}
