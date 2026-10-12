package com.hamza.ebankservice;

import com.hamza.ebankservice.entities.BankAccount;
import com.hamza.ebankservice.services.EbankService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
@EnableFeignClients
public class EbankServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(EbankServiceApplication.class, args);
    }

    @Bean
    CommandLineRunner start (EbankService ebankService) {
        return args -> {
            for(int i = 1 ; i <= 3 ; i++) {
                for (int j = 0 ; j < 5 ; j++) {
                    ebankService.saveBankAccount(BankAccount.builder()
                                    .type(Math.random()>0.5 ? "CURRENT-ACCOUNT" : "SAVING-ACCOUNT")
                                    .balance(Math.random()*16000)
                                    .customerId(i)
                            .build());
                }
            }
        };
    }
}
