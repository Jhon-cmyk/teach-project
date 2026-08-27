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

    public WebMvcConfig(SessionAuthenticationInterceptor sessionAuthenticationInterceptor) {
        this.sessionAuthenticationInterceptor = sessionAuthenticationInterceptor;
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
    }
}
