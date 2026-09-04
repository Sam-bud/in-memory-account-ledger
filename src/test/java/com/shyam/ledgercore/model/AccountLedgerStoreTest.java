package com.shyam.ledgercore.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountLedgerStoreTest {

    @Test
    void closingBalance_sumsCreditsAndDebitsUpToGivenDay() {
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new CreditEvent("E1", "ACC-001", 1, 1, new BigDecimal("1200.00")));
        store.appendEvent(new DebitEvent("E2", "ACC-001", 1, 1, new BigDecimal("950.00"), false));

        BigDecimal balance = store.getClosingBalanceAsOf(1);

        assertEquals(new BigDecimal("250.00"), balance);
    }

    @Test
    void closingBalance_excludesEventsWithValueDateAfterGivenDay() {
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new CreditEvent("E1", "ACC-001", 1, 1, new BigDecimal("1200.00")));
        store.appendEvent(new CreditEvent("E4", "ACC-001", 3, 3, new BigDecimal("400.00")));

        BigDecimal balanceAtDay1 = store.getClosingBalanceAsOf(1);

        assertEquals(new BigDecimal("1200.00"), balanceAtDay1);
    }

    @Test
    void closingBalance_reflectsBackdatedDebit_onceAppended() {
        // Mirrors E7: DEBIT arrives later but is value-dated to an earlier day.
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new CreditEvent("E1", "ACC-001", 1, 1, new BigDecimal("1200.00")));
        store.appendEvent(new DebitEvent("E2", "ACC-001", 1, 1, new BigDecimal("950.00"), false));
        store.appendEvent(new DebitEvent("E7", "ACC-001", 5, 2, new BigDecimal("620.00"), false));

        BigDecimal balanceAtDay2 = store.getClosingBalanceAsOf(2);

        assertEquals(new BigDecimal("-370.00"), balanceAtDay2);
    }
}