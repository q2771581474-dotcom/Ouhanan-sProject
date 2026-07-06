package com.ts.quotesystem.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 管理者ログイン要求DTO
 */
@Data
public class LoginRequest {
    @NotBlank(message = "ユーザー名は必須です。")
    private String username;

    @NotBlank(message = "パスワードは必須です。")
    private String password;
}
