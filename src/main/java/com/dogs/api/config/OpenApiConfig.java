package com.dogs.api.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Dogs API",
                description = "Backend APIs for managing dogs in the police force",
                version = "1.0.0"
        )
)
public class OpenApiConfig {
}
