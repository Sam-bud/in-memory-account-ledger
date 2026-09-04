package com.shyam.ledgercore.model;

import java.math.BigDecimal;

public record SettlementEvent(
        String eventId,
        String accountId,
        int arrivalDay,
        int valueDate,
        String referencedAuthId,
        BigDecimal settledAmount
) implements LedgerEvent {
}