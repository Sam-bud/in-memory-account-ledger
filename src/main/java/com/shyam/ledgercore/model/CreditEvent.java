package com.shyam.ledgercore.model;

import java.math.BigDecimal;

public record CreditEvent(
        String eventId,
        String accountId,
        int arrivalDay,
        int valueDate,
        BigDecimal amount
) implements LedgerEvent {
}
