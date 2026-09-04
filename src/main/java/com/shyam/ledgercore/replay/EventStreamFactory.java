package com.shyam.ledgercore.replay;

import com.shyam.ledgercore.model.*;
import java.math.BigDecimal;
import java.util.List;

public class EventStreamFactory {

    /**
     * The fixed 10-event stream, E1-E10, in the exact given arrival
     * order (list order = arrival order, per AMBIGUITIES.md — NOT
     * value_date order).
     */
    public static List<LedgerEvent> buildEventStream() {
        return List.of(
                new CreditEvent("E1", "ACC-001", 1, 1, new BigDecimal("1200.00")),
                new DebitEvent("E2", "ACC-001", 1, 1, new BigDecimal("950.00"), false),
                new AuthorizationEvent("E3", "ACC-001", 2, 2, "Auth-A", new BigDecimal("200.00")),
                new CreditEvent("E4", "ACC-001", 3, 3, new BigDecimal("400.00")),
                new SettlementEvent("E5", "ACC-001", 4, 4, "Auth-A", new BigDecimal("185.00")),
                new SettlementEvent("E6", "ACC-001", 4, 4, "Auth-Z", new BigDecimal("180.00")),
                new DebitEvent("E7", "ACC-001", 5, 2, new BigDecimal("620.00"), false),
                new AuthorizationEvent("E8", "ACC-001", 5, 5, "Auth-B", new BigDecimal("90.00")),
                new ReversalEvent("E9", "ACC-001", 6, 2, "E7"),
                new CreditEvent("E10-1", "ACC-002", 5, 5, new BigDecimal("3.333")),
                new CreditEvent("E10-2", "ACC-002", 5, 5, new BigDecimal("3.333")),
                new CreditEvent("E10-3", "ACC-002", 5, 5, new BigDecimal("3.334"))
        );
    }
}