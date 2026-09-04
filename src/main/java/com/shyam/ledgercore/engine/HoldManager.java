package com.shyam.ledgercore.engine;

import com.shyam.ledgercore.model.AccountLedgerStore;
import com.shyam.ledgercore.model.ActiveHold;
import java.math.BigDecimal;
import java.util.List;

/**
 * Coordinates hold-related queries for a given account. Does not
 * duplicate logic already in AccountLedgerStore/RuleEngine — delegates
 * to them. Exists as the single entry point callers (e.g. report
 * generation, event processing) use for hold-related questions, rather
 * than each caller reaching into AccountLedgerStore and RuleEngine
 * separately.
 */
public class HoldManager {

    /**
     * Available balance = ledger balance minus sum of active holds,
     * as of a given day.
     */
    public BigDecimal getAvailableBalance(AccountLedgerStore store, int day) {
        BigDecimal ledgerBalance = store.getClosingBalanceAsOf(day);

        BigDecimal totalActiveHolds = BigDecimal.ZERO;
        for (ActiveHold hold : store.getActiveHolds(day)) {
            totalActiveHolds = totalActiveHolds.add(hold.authorization().holdAmount());
        }

        return ledgerBalance.subtract(totalActiveHolds);
    }

    /**
     * All holds currently active as of a given day — delegates directly
     * to AccountLedgerStore, which already tracks lifecycle state
     * (ACTIVE until settled/reversed).
     */
    public List<ActiveHold> getActiveHolds(AccountLedgerStore store, int day) {
        return store.getActiveHolds(day);
    }
}