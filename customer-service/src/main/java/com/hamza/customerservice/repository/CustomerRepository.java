package com.hamza.customerservice.repository;

import com.hamza.customerservice.entities.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerRepository extends JpaRepository <Customer,Long> {
}
