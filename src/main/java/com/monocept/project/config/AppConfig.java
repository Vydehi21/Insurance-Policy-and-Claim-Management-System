package com.monocept.project.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {
        ModelMapper mapper = new ModelMapper();
        
        // Ensures strict property matching to avoid mismatching nested IDs
        mapper.getConfiguration()
              .setFieldMatchingEnabled(true)
              .setFieldAccessLevel(org.modelmapper.config.Configuration.AccessLevel.PRIVATE);
              
        return mapper;
    }
}
