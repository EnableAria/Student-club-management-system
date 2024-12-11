package com.liu.club_ms.controller;

import com.liu.club_ms.jwt.JWUtil;
import com.liu.club_ms.model.User;
import com.liu.club_ms.service.MailService;
import com.liu.club_ms.service.RedisService;
import com.liu.club_ms.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/login")
public class LoginController {

    @Resource
    private UserService userService;

    @Resource
    private MailService mailService;

    @Resource
    private RedisService redisService;

    //登录验证
    @PostMapping("/loginIn")
    public Map<String, Object> loginIn(@RequestBody User user){
        User user1 = userService.queryByNamePwdType(user);
        Map<String, Object> response = new HashMap<>();
        if(user1 != null){
            response.put("token", JWUtil.generateToken(user1));
//          response.put("userId", user1.getUserId());
//          response.put("userName", user1.getUserName());
//          response.put("Type", user1.getType());
            response.put("code", 20000);
        }else {
            response.put("code", 300);
            response.put("message", "用户名或者密码错误!");
        }
        return response;
    }

    @PostMapping("/sendCaptcha")
    public Map<String, Object> sendCaptcha(@RequestParam String email) {
        Map<String, Object> response = new HashMap<>();
        try {
            // 生成验证码
            String captcha = mailService.generateCaptcha(6);

            // 发送邮件
            mailService.sendCaptcha(email, "验证码邮件", captcha);

            // 存储到 Redis，有效期 5 分钟
            redisService.saveCaptcha(email, captcha, 300);

            response.put("success", true);
            response.put("message", "验证码已发送！");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            response.put("success", false);
            response.put("message", "验证码发送失败！");
        }
        return response;
    }

    @PostMapping("/register")
    public Map<String, Object> validateCaptcha(@RequestParam String email, @RequestParam String captcha) {
        Map<String, Object> response = new HashMap<>();
        String storedCaptcha = redisService.getCaptcha(email);

        if (storedCaptcha != null && storedCaptcha.equals(captcha)) {
            redisService.deleteCaptcha(email); // 验证成功后删除验证码
            response.put("success", true);
            response.put("message", "验证码验证成功！");
        } else {
            response.put("success", false);
            response.put("message", "验证码错误或已过期！");
        }
        return response;
    }

    //通过token获取管理员对象和角色信息
    @GetMapping("/queryUserInfo")
    public Map<String, Object> queryUserInfo(String token){
        Map<String, Object> response = new HashMap<>();

        Integer type = JWUtil.getType(token);
        List<String> list = new ArrayList<>();

        if(type == 2){
            list.add("Super");
        }else if(type == 1){
            list.add("Admin");
        }else if(type == 0){
            list.add("User");
        }

        response.put("code", 20000);
        Map<String, Object> data = new HashMap<>();
        data.put("roles", list);
        response.put("data", data);
        return response;
    }

    //退出
    @PostMapping("/loginOut")
    public Map<String, Object> logout(){
        Map<String, Object> response = new HashMap<>();
        response.put("code", 20000);
        return response;
    }
}