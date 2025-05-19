package com.codingtracker.security;

import com.codingtracker.util.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String token = getTokenFromRequest(request);

        if (token != null) {
            try {
                Jws<Claims> claimsJws = Jwts.parserBuilder()
                        .setSigningKey(JwtUtils.getSecretKey())
                        .build()
                        .parseClaimsJws(token);

                String username = claimsJws.getBody().getSubject();

                // 设置认证信息到SecurityContext
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(username, null, null);
                SecurityContextHolder.getContext().setAuthentication(authentication);

            } catch (JwtException e) {
                logger.error("Invalid JWT token", e);
            }
        }

        // 放行过滤器链
        filterChain.doFilter(request, response);
    }

    // 从请求头中获取token，格式：Bearer token...
    private String getTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // 去掉"Bearer "前缀
        }
        return null;
    }
}
