package com.monocept.project.config;

import org.modelmapper.ModelMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.monocept.project.model.Claim;
import com.monocept.project.dto.ClaimResponseDTO;
import com.monocept.project.dto.CustomerResponseDTO;
import com.monocept.project.model.Customer;
import com.monocept.project.model.PolicyPlan;
import com.monocept.project.dto.PolicyPlanResponseDTO;

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
                    
                    m.map(
                    	    src -> src.getUser().getId(),
                    	    CustomerResponseDTO::setUserId
                    	);

                    	m.map(
                    	    src -> src.getUser().getActiveStatus(),
                    	    CustomerResponseDTO::setActiveStatus
                    	);
                   
                });
        
        mapper.typeMap(Claim.class, ClaimResponseDTO.class)
        .addMappings(m -> {

            m.map(
                    src -> src.getPolicy()
                            .getCustomer()
                            .getUser()
                            .getFullName(),

                    ClaimResponseDTO::setCustomerName
            );

            m.map(
                    src -> src.getPolicy()
                            .getPolicyNumber(),

                    ClaimResponseDTO::setPolicyNumber
            );
        });

        // Needed so the frontend can round-trip productId on plan edits —
        // PolicyPlanResponseDTO previously only exposed productName/productType,
        // so the edit form had no way to resend the required productId.
        mapper.typeMap(PolicyPlan.class, PolicyPlanResponseDTO.class)
        .addMappings(m -> {

            m.map(
                    src -> src.getInsuranceProduct().getId(),

                    PolicyPlanResponseDTO::setProductId
            );
        });

        return mapper;
    }
}