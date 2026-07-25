package com.quant.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class QuantServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuantServerApplication.class, args);
    }
}
