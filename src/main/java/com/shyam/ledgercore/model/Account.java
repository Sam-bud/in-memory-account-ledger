package com.shyam.ledgercore.model;

public class Account {
    private final String accountId;
    private final Currency currency;

    public Account(String accountId, Currency currency) {
        this.accountId = accountId;
        this.currency = currency;
    }

    public String getAccountId() {
        return accountId;
    }

    public Currency getCurrency() {
        return currency;
    }
}