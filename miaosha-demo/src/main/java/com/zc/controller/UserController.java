package com.zc.controller;

import com.zc.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;

/**
 * @program: MiaoSha
 * @description:
 * @author: ZC
 * @create: 2023-07-19 19:13
 **/
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserService userService;
    private static final Logger LOGGER= LoggerFactory.getLogger(UserController.class);
    @GetMapping("/getVerifyHash/{sid}/{userId}")
    @ResponseBody
    public String getVerifyHash(@PathVariable("sid") Integer sid,
                                @PathVariable("userId") Integer userId){
        String hash;
        try{
            hash= userService.getVerifyHash(sid,userId);
        }catch (Exception e){
            LOGGER.error("获取验证hash失败，原因：[{}]",e.getMessage());
            return  "获取验证hash失败";
        }
        return String.format("请求抢购验证hash值为：%s",hash);
    }

    @GetMapping("/login/{userName}/{passWord}")
    public String login(@PathVariable("userName") String userName,
                        @PathVariable("passWord") String passWord){
        return userService.login(userName,passWord);
    }
}
