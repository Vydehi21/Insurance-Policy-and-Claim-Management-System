package com.monocept.project.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.monocept.project.dto.CustomerResponseDTO;
import com.monocept.project.model.Customer;

@Configuration
public class AppConfig {

    @Bean
    public ModelMapper modelMapper() {

        ModelMapper mapper = new ModelMapper();

        mapper.typeMap(Customer.class, CustomerResponseDTO.class)
                .addMappings(m -> {

                    m.map(
                        src -> src.getUser().getFullName(),
                        CustomerResponseDTO::setFullName
                    );

                    m.map(
                        src -> src.getUser().getEmail(),
                        CustomerResponseDTO::setEmail
                    );

                    m.map(
                        src -> src.getUser().getMobileNumber(),
                        CustomerResponseDTO::setMobileNumber
                    );
                });

        return mapper;
    }
}
