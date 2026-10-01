package com.yuetin.music.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Value("${yuetin.cors.allowed-origins:*}")
    private String allowedOrigins;

    @Value("${yuetin.audio-dir:./audio}")
    private String audioDir;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor).addPathPatterns("/**");
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 诗之音轨：朗诵/配乐/AI 生成音频，落盘在 yuetin.audio-dir（默认 backend/audio）
        String location = Path.of(audioDir).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/audio/**")
                .addResourceLocations(location.endsWith("/") ? location : location + "/")
                .setCachePeriod(86400);
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = "*".equals(allowedOrigins.trim())
                ? new String[]{"*"}
                : allowedOrigins.split("\\s*,\\s*");
        registry.addMapping("/**")
                .allowedOriginPatterns(origins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                // 允许前端读取自定义 token，前端便无需再存一份在 cookie
                .exposedHeaders("Authorization")
                .allowCredentials(true)
                .maxAge(3600);
    }
}
