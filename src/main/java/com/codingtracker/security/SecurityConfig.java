package com.codingtracker.security;

import com.codingtracker.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Autowired
    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 配置HTTP安全策略
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // 禁用 CSRF (一般用于API，不需要CSRF保护)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("api/auth/login", "api/auth/register").permitAll() // 允许 /login 和 /register 不需要认证
                        //.anyRequest().authenticated() // 其余所有请求都需要认证
                        .requestMatchers(HttpMethod.PUT).permitAll()  // 允许所有 PUT 请求
                        .requestMatchers(HttpMethod.DELETE).permitAll()  // 允许所有 DELETE 请求
                        .anyRequest().permitAll()
                )
                .httpBasic(AbstractHttpConfigurer::disable); // 禁用 HTTP Basic 验证
        return http.build(); // 构建并返回 SecurityFilterChain
    }

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
}