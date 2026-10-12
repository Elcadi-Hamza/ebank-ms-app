package com.hamza.customerservice.controllers;

import com.hamza.customerservice.entities.Customer;
import com.hamza.customerservice.service.CustomerService;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class CustomerRestController {
    private CustomerService customerService;

    public CustomerRestController(CustomerService customerService){
        this.customerService = customerService;
    }

    public List<Customer> getAllCustomers() {
        return customerService.getAllCustomers();
    }

    public Customer findCustomerById(Long id) {
        return customerService.findCustomerById(id);
    }

    public Customer saveCustomer(Customer customer){
        return customerService.saveCustomer(customer);
    }
}
