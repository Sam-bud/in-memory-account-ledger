package com.shyam.ledgercore.replay;

import com.shyam.ledgercore.engine.HoldManager;
import com.shyam.ledgercore.engine.ReversalHandler;
import com.shyam.ledgercore.engine.SettlementHandler;
import com.shyam.ledgercore.model.*;
import com.shyam.ledgercore.report.DailyReportGenerator;
import com.shyam.ledgercore.rules.RuleEngine;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reads the fixed event stream (in given arrival order — NOT value_date
 * order, per AMBIGUITIES.md) and dispatches each event to the correct
 * account's store, validating settlements/reversals before appending.
 *
 * After all events are replayed, drives the daily report for Day 1
 * through windowEndDay, including fee assessment.
 */
public class EventProcessor {

    private final SettlementHandler settlementHandler = new SettlementHandler();
    private final ReversalHandler reversalHandler = new ReversalHandler();
    private final RuleEngine ruleEngine = new RuleEngine();
    private final HoldManager holdManager = new HoldManager();

    public void replay(List<LedgerEvent> eventStream, Map<String, AccountLedgerStore> storesByAccountId) {
        for (LedgerEvent event : eventStream) {
            AccountLedgerStore store = storesByAccountId.get(event.accountId());

            switch (event) {
                case SettlementEvent s -> {
                    if (settlementHandler.isValidSettlement(store, s, s.arrivalDay())) {
                        store.appendEvent(s);
                    } else {
                        System.out.println("ERROR: " + s.eventId()
                                + " rejected — no active authorization for " + s.referencedAuthId());
                    }
                }
                case ReversalEvent r -> {
                    if (reversalHandler.isValidReversal(store, r)) {
                        store.appendEvent(r);
                    } else {
                        System.out.println("ERROR: " + r.eventId()
                                + " rejected — referenced event not found: " + r.referencedEventId());
                    }
                }
                 case AuthorizationEvent a -> {
                    if (ruleEngine.isAuthorizationApprovable(store, a.arrivalDay(), a.holdAmount())) {
                        store.appendEvent(a);
                    } else {
                        System.out.println("ERROR: " + a.eventId()
                                + " rejected — insufficient available balance");
                    }
                }
                default -> store.appendEvent(event); // CREDIT, DEBIT — no validation gate
            }
        }
    }

    public void assessFeesForDay(AccountLedgerStore store, int day) {
        BigDecimal fee = ruleEngine.assessOverdraftFee(store, day);
        if (fee != null) {
            store.appendEvent(new DebitEvent(
                    "FEE-" + store.getAccountId() + "-DAY" + day,
                    store.getAccountId(),
                    day,
                    day,
                    fee,
                    true
            ));
        }
    }
}