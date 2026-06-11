package com.ubs.wma.aat.rampuppack.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI document metadata. springdoc serves the spec at {@code /v3/api-docs} and Swagger UI
 * at {@code /swagger-ui.html} (see {@code springdoc.*} in application.yaml).
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    @Bean
    public OpenAPI rampUpPackOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Ramp-Up Pack Service API")
                        .version("v1")
                        .description("Reactive REST API for STAAT retention-pack email delivery: "
                                + "email templates, STAAT insight documents, live/batched sends "
                                + "and the send log.")
                        .contact(new Contact().name("UBS WMA AAT").email("wma-aat@ubs.com"))
                        .license(new License().name("UBS Internal — All rights reserved")));
    }
}
