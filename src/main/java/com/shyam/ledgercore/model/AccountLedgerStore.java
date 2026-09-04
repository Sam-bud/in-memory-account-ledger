package com.shyam.ledgercore.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class AccountLedgerStore {

    private final String accountId;
    private final List<LedgerEvent> events = new ArrayList<>();

    public AccountLedgerStore(String accountId) {
        this.accountId = accountId;
    }

    public void appendEvent(LedgerEvent event) {
        events.add(event);
    }

    public List<LedgerEvent> getAllEvents() {
        return Collections.unmodifiableList(events);
    }

    /**
     * Closing ledger balance as of a given day.
     *
     * INTERPRETATION CHOSEN (see AMBIGUITIES.md #9, #10):
     * Sums all events with valueDate <= day. Rule-agnostic — Rule Engine
     * decides whether fee/interest events get appended; this just sums
     * whatever exists in the store at call time.
     *
     * REVERSAL HANDLING:
     * A REVERSAL excludes its referenced event's contribution — but only if
     * the REVERSAL's own valueDate <= day too. This means a reversal doesn't
     * retroactively apply to a balance query for a day before the reversal
     * itself is value-dated to take effect.
     *
     * Single pass: for each event within the day window, if it's a REVERSAL,
     * track what it cancels; otherwise accumulate its amount — then subtract
     * out anything that ended up reversed within the same window.
     */
    public BigDecimal getClosingBalanceAsOf(int day) {
        java.util.Map<String, BigDecimal> contributions = new java.util.LinkedHashMap<>();
        java.util.Set<String> reversedIds = new java.util.HashSet<>();

        for (LedgerEvent event : events) {
            if (event.valueDate() > day) continue;

            switch (event) {
                case CreditEvent c -> contributions.put(c.eventId(), c.amount());
                case DebitEvent d -> contributions.put(d.eventId(), d.amount().negate());
                case SettlementEvent s -> contributions.put(s.eventId(), s.settledAmount().negate());
                case AuthorizationEvent a -> { /* holds never affect ledger balance */ }
                case ReversalEvent r -> reversedIds.add(r.referencedEventId());
            }
        }

        BigDecimal total = BigDecimal.ZERO;
        for (var entry : contributions.entrySet()) {
            if (!reversedIds.contains(entry.getKey())) {
                total = total.add(entry.getValue());
            }
        }
        return total;
    }

    /**
     * All holds currently ACTIVE (not yet settled or reversed) as of a given day.
     *
     * Mechanics only — does not attempt to resolve ambiguities #12/#13
     * (whether a hold's original approval should be revisited if later
     * backdated events change historical balance). This method only tracks
     * hold lifecycle state: ACTIVE until a SETTLEMENT or REVERSAL closes it.
     */
    public List<ActiveHold> getActiveHolds(int day) {
        Map<String, AuthorizationEvent> authorizationsByAuthId = new LinkedHashMap<>();
        Map<String, AuthorizationEvent> authorizationsByEventId = new LinkedHashMap<>();
        Set<String> closedAuthIds = new HashSet<>();

        for (LedgerEvent event : events) {
            if (event.valueDate() > day) continue;

            switch (event) {
                case AuthorizationEvent a -> {
                    authorizationsByAuthId.put(a.authId(), a);
                    authorizationsByEventId.put(a.eventId(), a);
                }
                case SettlementEvent s -> closedAuthIds.add(s.referencedAuthId());
                case ReversalEvent r -> closedAuthIds.add(r.referencedEventId());
                default -> { /* CREDIT/DEBIT don't affect hold state */ }
            }
        }

        List<ActiveHold> active = new ArrayList<>();
        for (var entry : authorizationsByAuthId.entrySet()) {
            String authId = entry.getKey();
            AuthorizationEvent auth = entry.getValue();
            boolean closedByAuthId = closedAuthIds.contains(authId);
            boolean closedByEventId = closedAuthIds.contains(auth.eventId());
            if (!closedByAuthId && !closedByEventId) {
                active.add(new ActiveHold(auth, HoldStatus.ACTIVE));
            }
        }
        return active;
    }

    public String getAccountId() {
        return accountId;
    }
}