package com.shyam.ledgercore.engine;

import com.shyam.ledgercore.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class SettlementHandlerTest {

    private final SettlementHandler settlementHandler = new SettlementHandler();

    @Test
    void isValidSettlement_true_whenReferencedAuthIsActive() {
        // Mirrors E3 (Auth-A) -> E5 settles it.
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new AuthorizationEvent("E3", "ACC-001", 2, 2, "Auth-A", new BigDecimal("200.00")));

        SettlementEvent settlement = new SettlementEvent("E5", "ACC-001", 4, 4, "Auth-A", new BigDecimal("185.00"));

        boolean valid = settlementHandler.isValidSettlement(store, settlement, 4);

        assertTrue(valid);
    }

    @Test
    void isValidSettlement_false_whenNoMatchingAuthorizationExists() {
        // Mirrors E6 (Auth-Z, never authorized).
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new AuthorizationEvent("E3", "ACC-001", 2, 2, "Auth-A", new BigDecimal("200.00")));

        SettlementEvent settlement = new SettlementEvent("E6", "ACC-001", 4, 4, "Auth-Z", new BigDecimal("180.00"));

        boolean valid = settlementHandler.isValidSettlement(store, settlement, 4);

        assertFalse(valid);
    }

    @Test
    void isValidSettlement_false_whenAuthorizationAlreadySettled() {
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new AuthorizationEvent("E3", "ACC-001", 2, 2, "Auth-A", new BigDecimal("200.00")));
        store.appendEvent(new SettlementEvent("E5", "ACC-001", 4, 4, "Auth-A", new BigDecimal("185.00")));

        // A second settlement attempt against the same, now-closed auth.
        SettlementEvent duplicateSettlement = new SettlementEvent("E5b", "ACC-001", 5, 5, "Auth-A", new BigDecimal("10.00"));

        boolean valid = settlementHandler.isValidSettlement(store, duplicateSettlement, 5);

        assertFalse(valid);
    }

    @Test
    void isValidSettlement_false_whenAuthorizationAlreadyReversed() {
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new AuthorizationEvent("E3", "ACC-001", 2, 2, "Auth-A", new BigDecimal("200.00")));
        store.appendEvent(new ReversalEvent("R1", "ACC-001", 3, 3, "Auth-A"));

        SettlementEvent settlement = new SettlementEvent("E5", "ACC-001", 4, 4, "Auth-A", new BigDecimal("185.00"));

        boolean valid = settlementHandler.isValidSettlement(store, settlement, 4);

        assertFalse(valid);
    }
}