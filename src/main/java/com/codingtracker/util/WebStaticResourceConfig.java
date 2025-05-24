package com.codingtracker.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebStaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir.windows:C:\\avatars\\}")
    private String windowsDir;

    @Value("${app.upload-dir.linux:/var/www/avatars/}")
    private String linuxDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String os = System.getProperty("os.name").toLowerCase();
        String path;
        if (os.contains("win")) {
            path = windowsDir;
        } else {
            path = linuxDir;
        }

        // 注意 file: 后面路径末尾要有斜杠
        registry.addResourceHandler("/avatars/**")
                .addResourceLocations("file:" + path);
    }
}
