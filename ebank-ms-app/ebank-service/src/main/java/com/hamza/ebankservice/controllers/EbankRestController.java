package com.hamza.ebankservice.controllers;

import com.hamza.ebankservice.entities.BankAccount;
import com.hamza.ebankservice.services.EbankService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class EbankRestController {
    EbankService ebankService;
    public EbankRestController(EbankService ebankService){
        this.ebankService = ebankService;
    }

    @GetMapping("/accounts")
    public List<BankAccount> getAllBankAccounts() {
        return ebankService.getAllBankAccounts();
    }

    @GetMapping("/accounts/{id}")
    public BankAccount getBankAccountById(@PathVariable String id) {
        return ebankService.getBankAccountById(id);
    }

    @PostMapping("/accounts")
    public BankAccount saveBankAccount(@RequestBody BankAccount bankAccount) {
        return ebankService.saveBankAccount(bankAccount);
    }

//    @GetMapping("/customer/accounts/{id}")
//    public List<BankAccount> getBankAccountsByCustomerId(@PathVariable Long id) {
//        return ebankService.getBankAccountsByCustomerId(id);
//    }
}
