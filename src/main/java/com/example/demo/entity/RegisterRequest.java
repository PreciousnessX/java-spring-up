package com.example.demo.entity;

import lombok.Data;

@Data
public class RegisterRequest {
    private String username;
    private String password;
    private String confirmPassword;
    private String email;
    private String phoneNumber;
    private String captchaId;    // 验证码ID
    private String captchaCode;  // 验证码
}