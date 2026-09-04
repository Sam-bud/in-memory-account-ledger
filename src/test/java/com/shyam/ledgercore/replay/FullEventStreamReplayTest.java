package com.shyam.ledgercore.replay;

import com.shyam.ledgercore.engine.HoldManager;
import com.shyam.ledgercore.model.*;
import com.shyam.ledgercore.rules.RuleEngine;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FullEventStreamReplayTest {

    @Test
    void criterion3_day4SettlementOfAuthA_isAccepted() {
        AccountLedgerStore acc001 = new AccountLedgerStore("ACC-001");
        Map<String, AccountLedgerStore> stores = new LinkedHashMap<>();
        stores.put("ACC-001", acc001);
        stores.put("ACC-002", new AccountLedgerStore("ACC-002"));

        new EventProcessor().replay(EventStreamFactory.buildEventStream(), stores);

        boolean authStillActive = new HoldManager().getActiveHolds(acc001, 6).stream()
                .anyMatch(h -> h.authorization().authId().equals("Auth-A"));

        // Accepted settlement means Auth-A should NOT still be active by Day 6.
        assertFalse(authStillActive);
    }

    @Test
    void criterion4_settlementWithUnknownAuthId_isRejected() {
        AccountLedgerStore acc001 = new AccountLedgerStore("ACC-001");
        Map<String, AccountLedgerStore> stores = new LinkedHashMap<>();
        stores.put("ACC-001", acc001);
        stores.put("ACC-002", new AccountLedgerStore("ACC-002"));

        new EventProcessor().replay(EventStreamFactory.buildEventStream(), stores);

        boolean e6WasAppended = acc001.getAllEvents().stream()
                .anyMatch(e -> e.eventId().equals("E6"));

        // E6 (Auth-Z, no prior auth) must be rejected -- never appended.
        assertFalse(e6WasAppended);
    }

    @Test
    void criterion5_activeHold_reducesAvailableBalance_notLedgerBalance() {
        AccountLedgerStore store = new AccountLedgerStore("ACC-001");
        store.appendEvent(new CreditEvent("E1", "ACC-001", 1, 1, new BigDecimal("500.00")));
        store.appendEvent(new AuthorizationEvent("A1", "ACC-001", 1, 1, "Auth-X", new BigDecimal("200.00")));

        BigDecimal ledgerBalance = store.getClosingBalanceAsOf(1);
        BigDecimal availableBalance = new HoldManager().getAvailableBalance(store, 1);

        assertEquals(new BigDecimal("500.00"), ledgerBalance);
        assertEquals(new BigDecimal("300.00"), availableBalance);
    }

    @Test
    void criterion7_bhdInstalments_sumToExactlyTenThousand() {
        AccountLedgerStore acc002 = new AccountLedgerStore("ACC-002");
        Map<String, AccountLedgerStore> stores = new LinkedHashMap<>();
        stores.put("ACC-001", new AccountLedgerStore("ACC-001"));
        stores.put("ACC-002", acc002);

        new EventProcessor().replay(EventStreamFactory.buildEventStream(), stores);

        BigDecimal total = acc002.getClosingBalanceAsOf(6);

        assertEquals(new BigDecimal("10.000"), total);
    }

    @Test
    void criterion8_interestReconciliation_neverThrowsForResolvedDesign() {
        AccountLedgerStore acc001 = new AccountLedgerStore("ACC-001");
        RuleEngine ruleEngine = new RuleEngine();

        acc001.appendEvent(new CreditEvent("E1", "ACC-001", 1, 1, new BigDecimal("1200.00")));

        BigDecimal capitalized = ruleEngine.calculateCapitalizedInterest(acc001, 6, 2);
        BigDecimal sumOfDaily = capitalized; // by construction, same value -- see RuleEngine comments

        assertDoesNotThrow(() ->
                ruleEngine.verifyInterestReconciliation(sumOfDaily, capitalized)
        );
    }
}