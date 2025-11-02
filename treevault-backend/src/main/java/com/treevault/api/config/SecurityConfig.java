package com.treevault.api.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class SecurityConfig {
    
    @Bean
    public FilterRegistrationBean<SecurityHeadersFilter> securityHeadersFilter() {
        FilterRegistrationBean<SecurityHeadersFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new SecurityHeadersFilter());
        registrationBean.addUrlPatterns("/*");
        registrationBean.setOrder(1);
        return registrationBean;
    }
    
    @Bean
    public FilterRegistrationBean<InputSanitizationFilter> inputSanitizationFilter() {
        FilterRegistrationBean<InputSanitizationFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new InputSanitizationFilter());
        registrationBean.addUrlPatterns("/api/*");
        registrationBean.setOrder(2);
        return registrationBean;
    }
    
    public static class SecurityHeadersFilter extends OncePerRequestFilter {
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       FilterChain filterChain) 
                throws ServletException, IOException {
            
            // Security headers
            response.setHeader("X-Content-Type-Options", "nosniff");
            response.setHeader("X-Frame-Options", "DENY");
            response.setHeader("X-XSS-Protection", "1; mode=block");
            response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            response.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
            
            filterChain.doFilter(request, response);
        }
    }
    
    public static class InputSanitizationFilter extends OncePerRequestFilter {
        
        @Override
        protected void doFilterInternal(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       FilterChain filterChain) 
                throws ServletException, IOException {
            
            // Sanitize request parameters
            HttpServletRequest sanitizedRequest = new SanitizedHttpServletRequestWrapper(request);
            
            filterChain.doFilter(sanitizedRequest, response);
        }
    }
    
    private static class SanitizedHttpServletRequestWrapper extends jakarta.servlet.http.HttpServletRequestWrapper {
        
        public SanitizedHttpServletRequestWrapper(HttpServletRequest request) {
            super(request);
        }
        
        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            
            String[] sanitized = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                sanitized[i] = sanitizeInput(values[i]);
            }
            return sanitized;
        }
        
        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return value != null ? sanitizeInput(value) : null;
        }
        
        private String sanitizeInput(String input) {
            if (input == null) {
                return null;
            }
            
            // Remove potentially dangerous characters
            return input
                .replaceAll("<script>", "")
                .replaceAll("</script>", "")
                .replaceAll("javascript:", "")
                .replaceAll("onerror=", "")
                .replaceAll("onload=", "")
                .trim();
        }
    }
}

