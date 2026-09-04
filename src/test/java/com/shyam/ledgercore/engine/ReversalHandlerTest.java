package com.shyam.ledgercore.engine;

import com.shyam.ledgercore.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ReversalHandlerTest {

    private final ReversalHandler reversalHandler = new ReversalHandler();

    @Test
    void isValidReversal_true_whenReferencedEventExists() {
        // Mirrors E9 reversing E7.
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new DebitEvent("E7", "ACC-001", 5, 2, new BigDecimal("620.00"), false));

        ReversalEvent reversal = new ReversalEvent("E9", "ACC-001", 6, 2, "E7");

        boolean valid = reversalHandler.isValidReversal(store, reversal);

        assertTrue(valid);
    }

    @Test
    void isValidReversal_false_whenReferencedEventDoesNotExist() {
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new DebitEvent("E7", "ACC-001", 5, 2, new BigDecimal("620.00"), false));

        ReversalEvent reversal = new ReversalEvent("R1", "ACC-001", 6, 2, "E999-DOES-NOT-EXIST");

        boolean valid = reversalHandler.isValidReversal(store, reversal);

        assertFalse(valid);
    }

    @Test
    void isValidReversal_true_whenReferencingAnAuthorizationEvent() {
        // A reversal can reference any prior event, not just money-moving ones.
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new AuthorizationEvent("E3", "ACC-001", 2, 2, "Auth-A", new BigDecimal("200.00")));

        ReversalEvent reversal = new ReversalEvent("R1", "ACC-001", 3, 2, "E3");

        boolean valid = reversalHandler.isValidReversal(store, reversal);

        assertTrue(valid);
    }
}