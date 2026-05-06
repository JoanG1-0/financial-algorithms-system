package com.financial.etl;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EtlServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EtlServiceApplication.class, args);
    }
}
