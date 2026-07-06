package com.ts.quotesystem.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger/OpenAPIの設定クラス
 * 管理者API向けにJWT Bearer認証の入力欄を有効化する
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "BearerAuth";
        
        return new OpenAPI()
                .info(new Info()
                        .title("自動車保険見積システム API仕様書")
                        .version("1.0")
                        .description("自動車保険見積システムのREST API仕様書。一般利用者向けの計算API、および管理者向け履歴・料率管理APIを含みます。"))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("管理者ログインAPIから取得したJWTトークンを入力してください（例: Bearer不要、トークン本体のみ）。")
                        ));
    }
}
