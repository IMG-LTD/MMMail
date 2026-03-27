package com.mmmail.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.mmmail")
public class MmmailServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(MmmailServerApplication.class, args);
    }
}
