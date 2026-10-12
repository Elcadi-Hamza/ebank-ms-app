package com.hamza.ebankservice.services;

import com.hamza.ebankservice.entities.BankAccount;
import com.hamza.ebankservice.repository.BankAccountRepository;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class EbankService {
    BankAccountRepository bankAccountRepository;
    public EbankService(BankAccountRepository bankAccountRepository) {
        this.bankAccountRepository = bankAccountRepository;
    }

    public List<BankAccount> getAllBankAccounts() {
        return bankAccountRepository.findAll();
    }

    public BankAccount getBankAccountById(String id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("bankAccount not found"));
    }

    public BankAccount saveBankAccount(BankAccount bankAccount) {
        bankAccount.setId(UUID.randomUUID().toString());
        bankAccount.setCreatedAt(new Date());
        return bankAccountRepository.save(bankAccount);
    }

    public List<BankAccount> getBankAccountsByCustomerId(Long id) {
        return bankAccountRepository.findByCustomerId(id);
    }
}
