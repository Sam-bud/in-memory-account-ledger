package com.shyam.ledgercore.engine;

import com.shyam.ledgercore.model.AccountLedgerStore;
import com.shyam.ledgercore.model.LedgerEvent;
import com.shyam.ledgercore.model.ReversalEvent;

/**
 * Validates a REVERSAL against the ledger's history before it's
 * allowed to be appended.
 *
 * NOT TESTED BY THE GIVEN EVENT STREAM: unlike SettlementHandler
 * (which handles E6's reversal-of-nonexistent-auth case explicitly),
 * no event in the given stream reverses a non-existent event. This
 * method still checks for it, for symmetry with SettlementHandler and
 * basic data integrity, but this exact path is unexercised by the
 * actual 10-event dataset.
 */
public class ReversalHandler {

    public boolean isValidReversal(AccountLedgerStore store, ReversalEvent reversal) {
        for (LedgerEvent event : store.getAllEvents()) {
            if (event.eventId().equals(reversal.referencedEventId())) {
                return true;
            }
        }
        return false;
    }
}