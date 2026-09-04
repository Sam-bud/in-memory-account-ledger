package com.shyam.ledgercore.model;

public record ReversalEvent(
        String eventId,
        String accountId,
        int arrivalDay,
        int valueDate,
        String referencedEventId
) implements LedgerEvent {
}