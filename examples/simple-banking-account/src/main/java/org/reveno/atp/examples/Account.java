/**
 *  Copyright (c) 2015 The original author or authors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0

 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


package org.reveno.atp.examples;

import java.util.Currency;

public class Account {

    public final String name;
    public final long balance;
    public final Currency currency;

    public Account add(long amount) {
        return new Account(name, balance + amount, currency);
    }

    public Account(String name, long initialBalance) {
        this(name, initialBalance, Currency.getInstance("USD"));
    }

    public Account(String name, long initialBalance, Currency currency) {
        this.name = name;
        this.balance = initialBalance;
        this.currency = currency;
    }

}