package com.hamza.ebankservice.services;

import com.hamza.ebankservice.entities.BankAccount;
import com.hamza.ebankservice.feign.CustomerRestClient;
import com.hamza.ebankservice.model.Customer;
import com.hamza.ebankservice.repository.BankAccountRepository;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class EbankService {
    BankAccountRepository bankAccountRepository;
    CustomerRestClient customerRestClient;
    public EbankService(BankAccountRepository bankAccountRepository, CustomerRestClient customerRestClient) {
        this.bankAccountRepository = bankAccountRepository;
        this.customerRestClient = customerRestClient;

    }

    @McpTool(description = "Get all Bank accounts")
    public List<BankAccount> getAllBankAccounts() {
        return bankAccountRepository.findAll();
    }

    @McpTool(description = "Get Bank Account by id")
    public BankAccount getBankAccountById(@McpToolParam(description = "The bank account id") String id) {
        BankAccount bankAccount =  bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("bankAccount not found"));
        bankAccount.setCustomer(customerRestClient.getCustomerById(bankAccount.getCustomerId()));
        return bankAccount;
    }

    @McpTool(description = "save a customer")
    public BankAccount saveBankAccount(@McpToolParam(description = "The bank account to save ()") BankAccount bankAccount) {
        try {
            Customer customer = customerRestClient.getCustomerById(bankAccount.getCustomerId());
            bankAccount.setId(UUID.randomUUID().toString());
            bankAccount.setCreatedAt(new Date());
            return bankAccountRepository.save(bankAccount);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());

        }
    }

//    public List<BankAccount> getBankAccountsByCustomerId(Long id) {
//        return bankAccountRepository.findByCustomerId(id);
//    }
}
