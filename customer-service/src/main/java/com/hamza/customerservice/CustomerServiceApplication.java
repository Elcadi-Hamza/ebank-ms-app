package com.hamza.customerservice;

import com.hamza.customerservice.entities.Customer;
import com.hamza.customerservice.service.CustomerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class CustomerServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(CustomerServiceApplication.class, args);


	}
	@Bean
	CommandLineRunner commandLineRunner (CustomerService customerService) {
		return args -> {
			List<String> names = List.of("name1", "name2", "name3");
			names.forEach(name->{
				customerService.saveCustomer(Customer.builder()
						.name(name).email(name+"@gmail.com")
						.build());
			});
		};
	}

}
