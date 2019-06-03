package org.reveno.atp.examples;

import java.util.Currency;

public class Account {

    public final String name;
    public final long balance;
    public final Currency currency;

    public Account(String name, long initialBalance) {
        this(name, initialBalance, Currency.getInstance("USD"));
    }

    public Account(String name, long initialBalance, Currency currency) {
        this.name = name;
        this.balance = initialBalance;
        this.currency = currency;
    }

    public Account add(long amount) {
        return new Account(name, balance + amount, currency);
    }

}