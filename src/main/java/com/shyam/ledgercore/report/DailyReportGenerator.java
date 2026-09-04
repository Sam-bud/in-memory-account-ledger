package com.shyam.ledgercore.report;

import com.shyam.ledgercore.engine.HoldManager;
import com.shyam.ledgercore.model.AccountLedgerStore;
import com.shyam.ledgercore.model.ActiveHold;
import com.shyam.ledgercore.rules.RuleEngine;
import java.math.BigDecimal;
import java.util.List;

/**
 * Iterates Day 1 through a given end day and prints, per account per
 * day: closing ledger balance, fee assessments, authorization states,
 * and errors.
 *
 * Does not decide business rules itself — queries AccountLedgerStore,
 * RuleEngine, and HoldManager, and formats what they return.
 */
public class DailyReportGenerator {

    private final RuleEngine ruleEngine;
    private final HoldManager holdManager;

    public DailyReportGenerator(RuleEngine ruleEngine, HoldManager holdManager) {
        this.ruleEngine = ruleEngine;
        this.holdManager = holdManager;
    }

    public void printDailyReport(AccountLedgerStore store, int day) {
        BigDecimal closingBalance = store.getClosingBalanceAsOf(day);
        List<ActiveHold> activeHolds = holdManager.getActiveHolds(store, day);

        System.out.println("Account: " + store.getAccountId() + " | Day " + day);
        System.out.println("  Closing ledger balance: " + closingBalance);

        System.out.println("  Active holds: " + activeHolds.size());
        for (ActiveHold hold : activeHolds) {
            System.out.println("    " + hold.authorization().authId()
                    + " (" + hold.authorization().holdAmount() + ") - " + hold.status());
        }
    }
}