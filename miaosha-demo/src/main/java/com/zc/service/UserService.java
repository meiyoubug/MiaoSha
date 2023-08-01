package com.zc.service;

import com.zc.entity.User;
import com.baomidou.mybatisplus.extension.service.IService;

/**
 * <p>
 *  服务类
 * </p>
 *
 * @author admin
 * @since 2023-07-14
 */
public interface UserService extends IService<User> {
     String getVerifyHash(Integer sid,Integer userId) throws Exception;

     /**
      * 登录
      *
      * @param userName 用户名
      * @param passWord 通过单词
      * @return {@link String}
      */
     String login(String userName,String passWord);
}
