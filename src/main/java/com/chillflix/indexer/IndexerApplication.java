package com.chillflix.indexer;

import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jmx.JmxAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.web.reactive.config.EnableWebFlux;

@SpringBootApplication(exclude = {JmxAutoConfiguration.class})
@EnableWebFlux
@EnableCaching
public class IndexerApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(IndexerApplication.class);
        
        // Desactiva los beans que no necesitas inmediatamente
        app.setLazyInitialization(true);
        
        // Reduce el logging durante el inicio
        app.setLogStartupInfo(false);
        
        // Desactiva el banner de Spring Boot
        app.setBannerMode(Banner.Mode.OFF);
        
        app.run(args);
    }
}