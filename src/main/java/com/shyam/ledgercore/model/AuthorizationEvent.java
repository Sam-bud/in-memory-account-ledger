package com.shyam.ledgercore.model;

import java.math.BigDecimal;

public record AuthorizationEvent(
        String eventId,
        String accountId,
        int arrivalDay,
        int valueDate,
        String authId,
        BigDecimal holdAmount
) implements LedgerEvent {
}