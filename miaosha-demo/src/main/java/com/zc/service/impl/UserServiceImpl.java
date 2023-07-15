package com.zc.service.impl;

import com.zc.entity.User;
import com.zc.mapper.UserMapper;
import com.zc.service.UserService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author admin
 * @since 2023-07-14
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

}
