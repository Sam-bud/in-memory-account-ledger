package com.shyam.ledgercore.model;

public enum Currency {
    AED(2),
    BHD(3);

    private final int decimalPlaces;

    Currency(int decimalPlaces) {
        this.decimalPlaces = decimalPlaces;
    }

    public int getDecimalPlaces() {
        return decimalPlaces;
    }
}