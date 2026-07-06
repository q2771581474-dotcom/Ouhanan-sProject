package com.ts.quotesystem.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * JWTのエンコード・デコードを行うユーティリティクラス
 */
@Component
public class JwtUtil {

    private final Key key;
    private final long expiration;

    // application.ymlからシークレットキーと有効期間を取得
    public JwtUtil(@Value("${jwt.secret}") String secret, @Value("${jwt.expiration}") long expiration) {
        byte[] decodedKey = Base64.getDecoder().decode(secret);
        this.key = Keys.hmacShaKeyFor(decodedKey);
        this.expiration = expiration;
    }

    /**
     * 指定されたユーザー名でJWTトークンを生成する
     * 【セキュリティ設計】HS256アルゴリズムと改ざん防止署名を用いて、ユーザーIDおよび有効期限を包含した改変不可能なトークンを発行する。
     */
    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expiration))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * トークンからユーザー名を取得する
     * 【設計考量】署名の検証を同時に伴うクレーム解析を行い、安全にトークン主体(Subject)を抽出する。
     */
    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * トークンの有効性をチェックする
     * 【セキュリティ設計】署名の妥当性、有効期限切れ(Expiration)、ユーザー名の合致を総合的に検証し、なりすましアクセスを阻止する。
     */
    public boolean validateToken(String token, String username) {
        try {
            Claims claims = getClaims(token);
            String tokenUsername = claims.getSubject();
            boolean isExpired = claims.getExpiration().before(new Date());
            return (tokenUsername.equals(username) && !isExpired);
        } catch (JwtException | IllegalArgumentException e) {
            // 不正なトークンの場合は検証失敗
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }
}
