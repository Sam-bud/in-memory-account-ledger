package com.shyam.ledgercore.model;

public record ActiveHold(
        AuthorizationEvent authorization,
        HoldStatus status
) {
}