package com.ts.quotesystem;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

/**
 * 自動車保険見積システム (Car Insurance Quote System) 起動クラス
 * EnableCaching: 料率マスタ取得API等のパフォーマンス向上のためのキャッシュを有効化する
 */
@SpringBootApplication
@EnableCaching
public class QuoteSystemApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuoteSystemApplication.class, args);
    }
}
