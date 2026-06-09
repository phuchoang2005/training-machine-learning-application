package com.example.aitraining.config;

import com.example.aitraining.auth.CurrentUserContext;
import com.example.aitraining.auth.UnauthorizedException;
import com.example.aitraining.repo.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class WebConfig extends OncePerRequestFilter {
    private final UserRepository users;

    public WebConfig(UserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        try {
            if (isPublicHealthCheck(request)) {
                chain.doFilter(request, response);
                return;
            }
            String header = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (header == null || !header.startsWith("Bearer ")) {
                throw new UnauthorizedException("Bearer token is required");
            }
            String token = header.substring("Bearer ".length()).trim();
            CurrentUserContext.set(users.findActiveByToken(token)
                    .orElseThrow(() -> new UnauthorizedException("Unknown or inactive bearer identity")));
            chain.doFilter(request, response);
        } finally {
            CurrentUserContext.clear();
        }
    }

    private boolean isPublicHealthCheck(HttpServletRequest request) {
        return "GET".equals(request.getMethod()) && "/api/v1/health".equals(request.getRequestURI());
    }
}
