package com.zc.service.impl;

import com.sun.org.apache.bcel.internal.generic.LOOKUPSWITCH;
import com.zc.entity.Stock;
import com.zc.entity.StockOrder;
import com.zc.mapper.StockMapper;
import com.zc.mapper.StockOrderMapper;
import com.zc.service.StockService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zc.utils.CacheKey;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
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
public class StockServiceImpl extends ServiceImpl<StockMapper, Stock> implements StockService {
    private static final Logger LOGGER = LoggerFactory.getLogger(StockServiceImpl.class);
    @Resource
    private StockMapper stockMapper;
    @Resource
    private StockOrderMapper stockOrderMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String SALT = CacheKey.HASH_KEY.getKey();

    private static final Integer ALLOW_COUNT = 10;

    @Override
    public void delStockCountCache(int sid) {
        String hashKey=CacheKey.GoodsKey.getKey()+"_"+sid;
        stringRedisTemplate.delete(hashKey);
        LOGGER.info("删除商品id：[{}]缓存",sid);
    }

    @Override
    public void setStockCountToCache(int sid, int count) {
        String hashKey = CacheKey.GoodsKey.getKey() + "_" + sid;
        stringRedisTemplate.opsForValue().set(hashKey, String.valueOf(count));
        LOGGER.info("Rides中存入的库存为：[{}]",count);
    }

    @Override
    public Integer getStockCountByCache(int sid) {
        String hashKey = CacheKey.GoodsKey.getKey() + "_" + sid;
        String count = stringRedisTemplate.opsForValue().get(hashKey);
        LOGGER.info("Rides中查询的库存为：[{}]",count);
        if(count==null) return null;
        return Integer.parseInt(count);
    }

    @Override
    public int getStockCountByDB(int sid) {
        Stock stock = stockMapper.selectById(sid);
        return stock.getCount() - stock.getSale();
    }

    @Override
    public int addUserCount(Integer userId) throws Exception {
        String limitKey = CacheKey.LIMIT_KEY.getKey() + "_" + userId;
        String limitNum = stringRedisTemplate.opsForValue().get(limitKey);
        int limit = -1;
        if (limitNum == null) {
            stringRedisTemplate.opsForValue().set(limitKey, "0", 3600, TimeUnit.SECONDS);
        } else {
            limit = Integer.parseInt(limitNum) + 1;
            stringRedisTemplate.opsForValue().set(limitKey, String.valueOf(limit), 3600, TimeUnit.SECONDS);
        }
        return limit == -1 ? 0 : limit;
    }

    @Override
    public boolean getUserIsBanned(Integer userId) {
        String limitKey = CacheKey.LIMIT_KEY.getKey() + "_" + userId;
        String limitNum = stringRedisTemplate.opsForValue().get(limitKey);
        if (limitNum == null) {
            LOGGER.error("该用户没有访问申请验证值记录，疑似异常");
            return true;
        }
        return Integer.parseInt(limitNum) > ALLOW_COUNT;
    }

    @Override
    public int createWrongOrder(int sid) {
        Stock stock = checkStock(sid);
        saleStock(sid);
        int id = createOrder(stock);
        return id;
    }

    @Override
    public int createOptimisticOrder(int sid) {
        //校验库存
        Stock stock = checkStock(sid);
        //乐观锁更新库存
        saleStockOptimistic(stock);
        //创建订单
        int id = createOrder(stock);
        return stock.getCount() - (stock.getSale() + 1);
    }

    @Override
    public int createVerifiedOrder(Integer sid, Integer userId, String verifyHash) throws Exception {
        //判断是否在秒杀时间内
        LOGGER.info("请自行验证是否在秒杀时间内");

        //验证hash值合法性
        String hashKey = CacheKey.HASH_KEY.getKey() + "_" + sid + "_" + userId;
        String verifyHashInRedis = stringRedisTemplate.opsForValue().get(hashKey);
        if (!verifyHash.equals(verifyHashInRedis)) {
            throw new Exception("hash与Redis中不符合");
        }
        LOGGER.info("验证hash值合法性成功");

        Stock stock = stockMapper.selectById(sid);
        //乐观锁更新库存
        saleStockOptimistic(stock);
        LOGGER.info("乐观锁更新库存成功");

        //创建订单
        StockOrder stockOrder = new StockOrder();
        stockOrder.setSid(sid);
        stockOrder.setName(stock.getName());
        stockOrder.setUserId(userId);
        stockOrderMapper.insert(stockOrder);
        LOGGER.info("创建订单成果");

        return stock.getCount() - (stock.getSale() + 1);


    }


    private void saleStockOptimistic(Stock stock) {
        LOGGER.info("查询数据库尝试更新库存");
        int count = stockMapper.updateByOptimistic(stock);
        if (count == 0) {
            throw new RuntimeException("并发更新库存失败，version不匹配");
        }
    }


    private Stock checkStock(int sid) {
        Stock stock = stockMapper.selectById(sid);
        if (stock.getSale() >= (stock.getCount())) {
            throw new RuntimeException("库存不足");
        }
        return stock;
    }


    private void saleStock(int sid) {
        LOGGER.info("扣减库存了");
        int cnt = stockMapper.updateSaleCnt(sid);
        if (cnt == 0) {
            throw new RuntimeException("没库存了");
        }
    }

    private int createOrder(Stock stock) {
        StockOrder stockOrder = new StockOrder();
        stockOrder.setSid(stock.getId());
        stockOrder.setName(stock.getName());
        int id = stockOrderMapper.insert(stockOrder);
        return id;
    }
}
