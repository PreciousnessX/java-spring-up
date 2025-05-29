package com.example.demo.entity;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String password;
    private String captchaId;    // 验证码ID
    private String captchaCode;  // 验证码
}
