package com.example.demo.controller;

import com.example.demo.entity.ApiResponse;
import com.example.demo.entity.LoginRequest;
import com.example.demo.entity.RegisterRequest;
import com.example.demo.service.UserService;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final DefaultKaptcha defaultKaptcha;
    private final StringRedisTemplate redisTemplate;
    private final UserService userService;

    /**
     * 生成验证码
     */
    @GetMapping("/captcha")
    public ApiResponse<Map<String, String>> getCaptcha() throws Exception {
        // 生成验证码文本
        String text = defaultKaptcha.createText();
        // 生成验证码ID
        String captchaId = UUID.randomUUID().toString();
        // 将验证码存入Redis，设置5分钟过期
        redisTemplate.opsForValue().set("captcha:" + captchaId, text, 5, TimeUnit.MINUTES);
        
        // 生成验证码图片
        BufferedImage image = defaultKaptcha.createImage(text);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", outputStream);
        
        // 将图片转换为Base64
        String base64Image = Base64.getEncoder().encodeToString(outputStream.toByteArray());
        
        // 构建返回结果
        Map<String, String> data = new HashMap<>();
        data.put("captchaId", captchaId);
        data.put("captchaImage", "data:image/jpeg;base64," + base64Image);
        
        return ApiResponse.success(data);
    }

    /**
     * 登录接口
     */
    @PostMapping("/login")
    public ApiResponse<?> login(@RequestBody LoginRequest loginRequest) {
        // 验证验证码
        String cachedCode = redisTemplate.opsForValue().get("captcha:" + loginRequest.getCaptchaId());
        if (cachedCode == null) {
            return ApiResponse.error("验证码已过期");
        }
        
        if (!cachedCode.equalsIgnoreCase(loginRequest.getCaptchaCode())) {
            return ApiResponse.error("验证码错误");
        }
        
        // 删除已使用的验证码
        redisTemplate.delete("captcha:" + loginRequest.getCaptchaId());
        
        // 验证用户
        Map<String, Object> loginResult = userService.validateLogin(loginRequest);
        if (!(boolean) loginResult.get("success")) {
            return ApiResponse.error((String) loginResult.get("message"));
        }
        
        return ApiResponse.success("登录成功", loginResult);
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ApiResponse<?> register(@RequestBody RegisterRequest registerRequest) {
        Map<String, Object> result = userService.register(registerRequest);
        
        if ((Boolean) result.get("success")) {
            return ApiResponse.success("注册成功", result);
        } else {
            return ApiResponse.error((String) result.get("message"));
        }
    }
}
