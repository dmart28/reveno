package org.reveno.atp.examples;

public class Account {

    public final String name;
    public final long balance;

    public Account add(long amount) {
        return new Account(name, balance + amount);
    }

    public Account(String name, long initialBalance) {
        this.name = name;
        this.balance = initialBalance;
    }

}