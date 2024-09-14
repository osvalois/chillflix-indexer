package com.chillflix.indexer.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        
        // Permite solicitudes desde el dominio específico de Chillflix
        config.setAllowedOrigins(Arrays.asList("https://chillflix.fly.dev"));
        
        // Permite los métodos HTTP comunes
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        
        // Permite headers comunes
        config.setAllowedHeaders(Arrays.asList("Authorization", "Content-Type", "X-Requested-With", "Accept"));
        
        // Expone headers específicos si es necesario
        config.setExposedHeaders(Arrays.asList("Content-Disposition"));
        
        // Permite credenciales si es necesario (cookies, autenticación HTTP, certificados TLS de cliente)
        config.setAllowCredentials(true);
        
        // Configura el tiempo máximo que el navegador puede cachear la respuesta pre-flight
        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}