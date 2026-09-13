package com.example.auto_repair_shop_api.service;

import com.example.auto_repair_shop_api.dto.LoginRequestDTO;

public interface AuthService {
    String login(LoginRequestDTO dto);
}