package com.ts.quotesystem.service;

import com.ts.quotesystem.dto.LoginRequest;
import com.ts.quotesystem.dto.LoginResponse;
import com.ts.quotesystem.entity.AdminUser;
import com.ts.quotesystem.repository.AdminUserRepository;
import com.ts.quotesystem.util.JwtUtil;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理者認証サービス
 */
@Service
@Transactional(readOnly = true)
public class AdminService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AdminService(AdminUserRepository adminUserRepository,
                        PasswordEncoder passwordEncoder,
                        JwtUtil jwtUtil) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    /**
     * 管理者ログインの検証とトークンの発行
     * 【セキュリティ設計】BCryptによるセキュアなパスワード照合を行い、認証成功時にはステートレスなJWTを発行することで、スケーラビリティとセキュアな通信を両立する。
     */
    public LoginResponse login(LoginRequest request) {
        AdminUser admin = adminUserRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new BadCredentialsException("ユーザー名またはパスワードが正しくありません。"));

        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new BadCredentialsException("ユーザー名またはパスワードが正しくありません。");
        }

        // JWTトークン生成
        String token = jwtUtil.generateToken(admin.getUsername());

        return new LoginResponse(token, admin.getUsername());
    }
}
