package com.kiddo.remotescreen.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class AuthSignalServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthSignalServerApplication.class, args);
    }

}
