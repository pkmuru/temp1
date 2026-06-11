package com.ubs.wma.aat.rampuppack;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Entry point for the Ramp-Up Pack Service.
 *
 * <p>Reactive (Spring WebFlux) REST microservice on Spring Boot 4 / Spring Framework 7,
 * using Spring Data R2DBC for non-blocking PostgreSQL access and Azure Identity for
 * Service Principal (SPN) authentication to the StaatEmail firm-wide email service.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class RampUpPackServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RampUpPackServiceApplication.class, args);
    }
}
