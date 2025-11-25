package com.chathub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS 跨域配置
 * 允許前端應用（localhost:3000）訪問後端 API
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 允許的來源
        configuration.setAllowedOrigins(Arrays.asList(
            "http://localhost:3000",
            "http://127.0.0.1:3000"
        ));

        // 允許的 HTTP 方法
        configuration.setAllowedMethods(Arrays.asList(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "OPTIONS",
            "PATCH"
        ));

        // 允許的請求標頭
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With"
        ));

        // 允許發送認證資訊（Cookie、Authorization header）
        configuration.setAllowCredentials(true);

        // 預檢請求的有效期（1小時）
        configuration.setMaxAge(3600L);

        // 暴露的響應標頭
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization"
        ));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 對所有路徑生效
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
