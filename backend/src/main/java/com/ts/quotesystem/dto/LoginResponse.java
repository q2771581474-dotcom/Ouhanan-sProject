package com.ts.quotesystem.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 管理者ログイン応答DTO
 */
@Data
@AllArgsConstructor
public class LoginResponse {
    private String token;
    private String username;
}
