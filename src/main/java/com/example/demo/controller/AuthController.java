package com.example.demo.controller;

import com.example.demo.entity.LoginRequest;
import com.example.demo.entity.RegisterRequest;
import com.example.demo.service.UserService;
import com.google.code.kaptcha.impl.DefaultKaptcha;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AuthController {

    private final DefaultKaptcha defaultKaptcha;
    private final StringRedisTemplate redisTemplate;
    private final UserService userService;

    /**
     * 生成验证码
     */
    @GetMapping("/captcha")
    public void getCaptcha(HttpServletResponse response) throws Exception {
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
        
        // 设置响应头
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        response.setHeader("Captcha-ID", captchaId);
        
        // 输出图片
        ServletOutputStream out = response.getOutputStream();
        out.write(outputStream.toByteArray());
        out.flush();
        out.close();
    }

    /**
     * 登录接口
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // 验证验证码
        String cachedCode = redisTemplate.opsForValue().get("captcha:" + loginRequest.getCaptchaId());
        if (cachedCode == null) {
            return ResponseEntity.badRequest().body("验证码已过期");
        }
        
        if (!cachedCode.equalsIgnoreCase(loginRequest.getCaptchaCode())) {
            return ResponseEntity.badRequest().body("验证码错误");
        }
        
        // 删除已使用的验证码
        redisTemplate.delete("captcha:" + loginRequest.getCaptchaId());
        
        // 这里添加实际的用户验证逻辑
        // 示例中仅做简单判断
        if ("admin".equals(loginRequest.getUsername()) && "password".equals(loginRequest.getPassword())) {
            Map<String, Object> result = new HashMap<>();
            result.put("token", UUID.randomUUID().toString());
            result.put("message", "登录成功");
            return ResponseEntity.ok(result);
        }
        
        return ResponseEntity.badRequest().body("用户名或密码错误");
    }
    
    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        Map<String, Object> result = userService.register(registerRequest);
        
        if ((Boolean) result.get("success")) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.badRequest().body(result);
        }
    }
}
