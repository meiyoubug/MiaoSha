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
}
