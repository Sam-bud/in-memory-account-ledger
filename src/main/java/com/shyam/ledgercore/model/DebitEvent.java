package com.shyam.ledgercore.model;

import java.math.BigDecimal;

public record DebitEvent(
        String eventId,
        String accountId,
        int arrivalDay,
        int valueDate,
        BigDecimal amount,
        boolean feeFlag
) implements LedgerEvent {
}
