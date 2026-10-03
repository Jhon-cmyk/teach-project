package com.ruyi.teach.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final SessionAuthenticationInterceptor sessionAuthenticationInterceptor;
    private final AiRateLimitInterceptor aiRateLimitInterceptor;

    public WebMvcConfig(SessionAuthenticationInterceptor sessionAuthenticationInterceptor,
                        AiRateLimitInterceptor aiRateLimitInterceptor) {
        this.sessionAuthenticationInterceptor = sessionAuthenticationInterceptor;
        this.aiRateLimitInterceptor = aiRateLimitInterceptor;
    }

    // 读取 yml 里配置的路径
    @Value("${ruyi.upload-path}")
    private String uploadPath;

    /**
     * 静态资源映射
     * 访问 http://localhost:8820/api/profile/xxx.jpg
     * 就会自动映射到 D:/teach/files/xxx.jpg
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        // 1. 放行 Knife4j (Swagger) 的静态网页资源
        registry.addResourceHandler("doc.html")
                .addResourceLocations("classpath:/META-INF/resources/");
        registry.addResourceHandler("/webjars/**")
                .addResourceLocations("classpath:/META-INF/resources/webjars/");
        // file: 表示文件协议
        registry.addResourceHandler("/profile/**")
                .addResourceLocations("file:" + uploadPath);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionAuthenticationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns("/actuator/health", "/actuator/health/**");

        // 必须晚于认证拦截器：限流按登录用户 ID 计数，依赖认证阶段绑定的身份。
        registry.addInterceptor(aiRateLimitInterceptor)
                .addPathPatterns("/ai/**")
                .order(1);
    }
}
