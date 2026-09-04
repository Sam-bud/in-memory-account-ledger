package com.shyam.ledgercore.rules;

import com.shyam.ledgercore.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RuleEngineTest {

    private final RuleEngine ruleEngine = new RuleEngine();

    @Test
    void assessOverdraftFee_returnsFee_whenBalanceNegative() {
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new DebitEvent("E7", "ACC-001", 5, 2, new BigDecimal("620.00"), false));

        BigDecimal fee = ruleEngine.assessOverdraftFee(store, 2);

        assertEquals(new BigDecimal("25.00"), fee);
    }

    @Test
    void assessOverdraftFee_returnsNull_whenBalancePositive() {
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new CreditEvent("E1", "ACC-001", 1, 1, new BigDecimal("1200.00")));

        BigDecimal fee = ruleEngine.assessOverdraftFee(store, 1);

        assertNull(fee);
    }

    @Test
    void assessOverdraftFee_returnsNull_whenFeeAlreadyChargedThatDay() {
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new DebitEvent("E7", "ACC-001", 5, 2, new BigDecimal("620.00"), false));
        store.appendEvent(new DebitEvent("FEE-ACC-001-DAY2", "ACC-001", 2, 2, new BigDecimal("25.00"), true));

        BigDecimal fee = ruleEngine.assessOverdraftFee(store, 2);

        assertNull(fee);
    }

    @Test
    void calculateDailyInterest_returnsAmount_whenBalancePositive() {
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new CreditEvent("E1", "ACC-001", 1, 1, new BigDecimal("1000.00")));

        BigDecimal interest = ruleEngine.calculateDailyInterest(store, 1, 2);

        // 1000.00 * 0.0004 = 0.40
        assertEquals(new BigDecimal("0.40"), interest);
    }

    @Test
    void calculateDailyInterest_returnsNull_whenBalanceNotPositive() {
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new DebitEvent("D1", "ACC-001", 1, 1, new BigDecimal("100.00"), false));

        BigDecimal interest = ruleEngine.calculateDailyInterest(store, 1, 2);

        assertNull(interest);
    }

    @Test
    void calculateCapitalizedInterest_sumsOnlyPositiveDays() {
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new CreditEvent("E1", "ACC-001", 1, 1, new BigDecimal("1000.00")));
        // Day 1 positive (1000.00), stays flat through day 2 (no new events) — both days count.

        BigDecimal capitalized = ruleEngine.calculateCapitalizedInterest(store, 2, 2);

        // Day1: 0.40, Day2: 0.40 -> total 0.80
        assertEquals(new BigDecimal("0.80"), capitalized);
    }

    @Test
    void verifyInterestReconciliation_throws_whenSumsDoNotMatch() {
        assertThrows(IllegalStateException.class, () ->
                ruleEngine.verifyInterestReconciliation(new BigDecimal("0.80"), new BigDecimal("0.79"))
        );
    }

    @Test
    void verifyInterestReconciliation_doesNotThrow_whenSumsMatch() {
        assertDoesNotThrow(() ->
                ruleEngine.verifyInterestReconciliation(new BigDecimal("0.80"), new BigDecimal("0.80"))
        );
    }

    @Test
    void isAuthorizationApprovable_true_whenAvailableBalanceStaysNonNegative() {
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new CreditEvent("E1", "ACC-001", 1, 1, new BigDecimal("500.00")));

        boolean approvable = ruleEngine.isAuthorizationApprovable(store, 1, new BigDecimal("200.00"));

        assertTrue(approvable);
    }

    @Test
    void isAuthorizationApprovable_false_whenAvailableBalanceWouldGoNegative() {
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new CreditEvent("E1", "ACC-001", 1, 1, new BigDecimal("300.00")));

        boolean approvable = ruleEngine.isAuthorizationApprovable(store, 1, new BigDecimal("400.00"));

        assertFalse(approvable);
    }
}