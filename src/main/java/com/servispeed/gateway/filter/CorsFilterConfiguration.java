package com.servispeed.gateway.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class CorsFilterConfiguration {

    @Bean
    public CorsFilter corsFilter() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration config = new CorsConfiguration();
        
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*"); // En producción cambia el "*" por la URL de tu frontend
        config.addAllowedHeader("*"); // Permitir todos los headers (Content-Type, Authorization, etc.)
        config.addAllowedMethod("*"); // Permitir GET, POST, PUT, DELETE, etc.
        
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}