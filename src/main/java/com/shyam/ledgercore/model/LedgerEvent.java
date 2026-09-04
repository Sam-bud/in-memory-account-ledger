package com.shyam.ledgercore.model;


public sealed interface LedgerEvent
        permits CreditEvent, DebitEvent, AuthorizationEvent, SettlementEvent, ReversalEvent {

    String eventId();
    String accountId();
    int arrivalDay();
    int valueDate();
}
