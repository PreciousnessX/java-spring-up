package com.example.demo.service;

import com.example.demo.entity.LoginRequest;
import com.example.demo.entity.RegisterRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * 验证登录
     */
    public Map<String, Object> validateLogin(LoginRequest request) {
        Map<String, Object> result = new HashMap<>();
        
        User user = userRepository.findByUsername(request.getUsername())
                .filter(u -> u.getPassword().equals(request.getPassword()))
                .orElse(null);
        
        if (user == null) {
            result.put("success", false);
            result.put("message", "用户名或密码错误");
            return result;
        }
        
        String token = UUID.randomUUID().toString();
        // TODO: 在实际生产环境中，应该使用更安全的方式生成和存储token
        
        result.put("success", true);
        result.put("message", "登录成功");
        result.put("token", token);
        result.put("userId", user.getId());
        result.put("username", user.getUsername());
        
        return result;
    }

    /**
     * 用户注册
     * @param request 注册请求
     * @return 注册结果
     */
    public Map<String, Object> register(RegisterRequest request) {
        Map<String, Object> result = new HashMap<>();

        // 验证验证码
        String cachedCode = redisTemplate.opsForValue().get("captcha:" + request.getCaptchaId());
        if (cachedCode == null) {
            result.put("success", false);
            result.put("message", "验证码已过期");
            return result;
        }

        if (!cachedCode.equalsIgnoreCase(request.getCaptchaCode())) {
            result.put("success", false);
            result.put("message", "验证码错误");
            return result;
        }

        // 删除已使用的验证码
        redisTemplate.delete("captcha:" + request.getCaptchaId());

        // 检查用户名是否已存在
        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            result.put("success", false);
            result.put("message", "用户名已存在");
            return result;
        }

        // 检查邮箱是否已存在
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            result.put("success", false);
            result.put("message", "邮箱已被注册");
            return result;
        }

        try {
            // 创建新用户
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword()); // TODO: 在生产环境中应该对密码进行加密
            user.setEmail(request.getEmail());
            
            // 保存用户
            userRepository.save(user);
            
            result.put("success", true);
            result.put("message", "注册成功");
            result.put("userId", user.getId());
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "注册失败：" + e.getMessage());
        }
        
        return result;
    }
}