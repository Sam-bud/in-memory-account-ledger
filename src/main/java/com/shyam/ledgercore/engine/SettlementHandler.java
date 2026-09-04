package com.shyam.ledgercore.engine;

import com.shyam.ledgercore.model.AccountLedgerStore;
import com.shyam.ledgercore.model.ActiveHold;
import com.shyam.ledgercore.model.SettlementEvent;

/**
 * Validates a SETTLEMENT against the ledger's current active holds
 * before it's allowed to be appended. Per acceptance criterion #4: any
 * settlement referencing an authorization ID not present (or no longer
 * active) must be rejected and the funds must not leave the account.
 */
public class SettlementHandler {

    /**
     * Returns true if this settlement references a currently ACTIVE
     * authorization (by authId), using AccountLedgerStore's own
     * active-hold tracking rather than re-scanning raw history.
     */
    public boolean isValidSettlement(AccountLedgerStore store, SettlementEvent settlement, int day) {
        for (ActiveHold hold : store.getActiveHolds(day)) {
            if (hold.authorization().authId().equals(settlement.referencedAuthId())) {
                return true;
            }
        }
        return false;
    }
}