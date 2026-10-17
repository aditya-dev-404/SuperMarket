package com.aditya.supermarket;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Entry point of the SuperMarket multi-vendor marketplace backend. */
@SpringBootApplication
public class SuperMarketApplication {

    public static void main(String[] args) {
        SpringApplication.run(SuperMarketApplication.class, args);
        System.out.println("Application Started...");
    }
}
