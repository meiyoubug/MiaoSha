package com.zc.service.impl;

import com.zc.entity.Stock;
import com.zc.entity.StockOrder;
import com.zc.entity.User;
import com.zc.mapper.StockMapper;
import com.zc.mapper.StockOrderMapper;
import com.zc.mapper.UserMapper;
import com.zc.result.Result;
import com.zc.result.ResultFactory;
import com.zc.service.StockOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zc.utils.CacheKey;
import com.zc.utils.UserHodler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author admin
 * @since 2023-07-14
 */
@Service
public class StockOrderServiceImpl extends ServiceImpl<StockOrderMapper, StockOrder> implements StockOrderService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockOrderServiceImpl.class);
    @Resource
    private UserMapper userMapper;
    @Resource
    private StockMapper stockMapper;
    @Resource
    private StockOrderMapper stockOrderMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public Result createOrderBySid(int sid) throws Exception {
        //从thread-local获取用户id
        Integer userId= Integer.valueOf(UserHodler.get());
        //缓存中的key
        String key=returnGoodsHashKey(sid);
        //检查库存
        boolean hasCount = checkCount(sid);
        if (!hasCount) {
            return ResultFactory.buildFailResult("库存不足");
        }

        //创建订单
        int cnt = createOrder(sid, userId);
        if (cnt == 0) {
            throw new Exception("发生意外错误，请稍后再试");
        }
        stringRedisTemplate.delete(key);
        //库存充足,开始售卖商品
        cnt = saleGoods(sid);
        if (cnt == 0) {
            throw new Exception("库存不足！");
        }
        return ResultFactory.buildSuccessResult("购买成功");
    }

    @Override
    public String returnGoodsHashKey(int sid) {
        return CacheKey.GoodsKey.getKey() + "_" + sid;
    }

    @Override
    public boolean checkCount(int sid) {
        // 获取库存
        int count = getCountFromCache(sid);
        if (count == 0) {
            return false;
        }
        return true;
    }


    @Override
    public void setCountToCache(int sid, int count) {
        String hashKey = returnGoodsHashKey(sid);
        stringRedisTemplate.opsForValue().set(hashKey, String.valueOf(count), 3600, TimeUnit.SECONDS);
    }

    @Override
    public int getCountFromCache(int sid) {
        String hashKey = returnGoodsHashKey(sid);
        String count = stringRedisTemplate.opsForValue().get(hashKey);
        //如果缓存命中
        if (count != null) {
            return Integer.parseInt(count);
        }
        //缓存不命中
        //去数据库查库存
        int cnt = getCountFromDB(sid);
        //将查到的缓存放入Cache
        setCountToCache(sid, cnt);
        return cnt;
    }

    @Override
    public void deleteCountFromCache(int sid) {
        String hashKey = returnGoodsHashKey(sid);
        stringRedisTemplate.delete(hashKey);
    }

    @Override
    public int getCountFromDB(int sid) {
        Stock stock = stockMapper.selectById(sid);
        return stock.getCount() - stock.getSale();
    }

    @Override
    public int saleGoods(int sid) {
        int cnt = stockMapper.updateSaleCnt(sid);
        return cnt;
    }

    @Override
    public int createOrder(int sid, int userId) {
        StockOrder stockOrder = new StockOrder();
        stockOrder.setSid(sid);
        stockOrder.setUserId(userId);
        int cnt = stockOrderMapper.insert(stockOrder);
        return cnt;
    }


}
