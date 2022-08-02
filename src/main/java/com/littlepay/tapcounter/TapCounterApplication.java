package com.littlepay.tapcounter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class TapCounterApplication {

    public static void main(String[] args) {
        SpringApplication.run(TapCounterApplication.class, args);
    }

}
