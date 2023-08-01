package com.zc.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.zc.entity.Stock;
import com.zc.mapper.StockMapper;
import com.zc.utils.CacheKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.zc.entity.User;
import com.zc.mapper.UserMapper;
import com.zc.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import java.util.UUID;
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
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);
    @Resource
    private UserMapper userMapper;
    @Resource
    private StockMapper stockMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String SALT = CacheKey.HASH_KEY.getKey();



    @Override
    public String getVerifyHash(Integer sid, Integer userId) throws Exception {

        //检查用户合法性
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new Exception("用户不存在");
        }
        LOGGER.info("用户信息：[{}]", user.toString());

        //检查商品合法性
        Stock stock = stockMapper.selectById(sid);
        if (stock == null) {
            throw new Exception("商品不存在");
        }
        LOGGER.info("商品信息：[{}]", stock.toString());

        //生成hash
        String verify = SALT + sid + userId;
        String verifyHash = DigestUtils.md5DigestAsHex(verify.getBytes());

        //将hash和用户商品信息存入redis
        String hashKey = CacheKey.HASH_KEY.getKey() + "_" + sid + "_" + userId;
        stringRedisTemplate.opsForValue().set(hashKey, verifyHash, 3600, TimeUnit.SECONDS);
        LOGGER.info("Redis写入：[{}] [{}]", hashKey, verifyHash);
        return verifyHash;
    }

    @Override
    public String login(String userName, String passWord) {
        User user=userMapper.selectOne(new QueryWrapper<User>().eq("user_name",userName));
        if(user==null){
            return "用户名或密码错误";
        }

        if(!passWord.equals(user.getPassWord())){
            return "用户名或密码错误";
        }

        String token= UUID.randomUUID().toString();
        String key=CacheKey.LOGIN_USER_KEY+token;
        stringRedisTemplate.opsForValue().set(key,user.getId().toString());
        stringRedisTemplate.expire(key,7,TimeUnit.DAYS);

        //返回token
        return token;
    }
}
