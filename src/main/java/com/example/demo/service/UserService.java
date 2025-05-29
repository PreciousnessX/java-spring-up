package com.example.demo.service;

import com.example.demo.entity.RegisterRequest;
import com.example.demo.entity.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;

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

        // 验证密码是否一致
        if (!request.getPassword().equals(request.getConfirmPassword())) {
            result.put("success", false);
            result.put("message", "两次输入的密码不一致");
            return result;
        }

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
            user.setPhoneNumber(request.getPhoneNumber());
            
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

    /**
     * 验证用户登录
     * @param username 用户名
     * @param password 密码
     * @return 用户对象，如果验证失败返回null
     */
    public User validateUser(String username, String password) {
        return userRepository.findByUsername(username)
                .filter(user -> user.getPassword().equals(password)) // TODO: 在生产环境中应该对密码进行加密比对
                .orElse(null);
    }
}