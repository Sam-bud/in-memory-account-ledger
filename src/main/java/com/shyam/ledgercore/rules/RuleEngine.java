package com.shyam.ledgercore.rules;

import com.shyam.ledgercore.model.AccountLedgerStore;
import com.shyam.ledgercore.model.DebitEvent;
import java.math.BigDecimal;
import java.math.RoundingMode;

public class RuleEngine {

    private static final BigDecimal OVERDRAFT_FEE_AED = new BigDecimal("25.00");
    private static final BigDecimal DAILY_INTEREST_RATE = new BigDecimal("0.0004");

    public boolean isAuthorizationApprovable(AccountLedgerStore store, int day, BigDecimal requestedHoldAmount) {
        BigDecimal ledgerBalance = store.getClosingBalanceAsOf(day);

        BigDecimal totalActiveHolds = BigDecimal.ZERO;
        for (var hold : store.getActiveHolds(day)) {
            totalActiveHolds = totalActiveHolds.add(hold.authorization().holdAmount());
        }

        BigDecimal availableBalance = ledgerBalance.subtract(totalActiveHolds);
        BigDecimal availableAfterNewHold = availableBalance.subtract(requestedHoldAmount);

        return availableAfterNewHold.compareTo(BigDecimal.ZERO) >= 0;
    }

    public BigDecimal assessOverdraftFee(AccountLedgerStore store, int day) {
        boolean feeAlreadyChargedToday = store.getAllEvents().stream()
                .anyMatch(e -> e instanceof DebitEvent d
                        && d.valueDate() == day
                        && d.feeFlag());

        if (feeAlreadyChargedToday) {
            return null;
        }

        BigDecimal closingBalance = store.getClosingBalanceAsOf(day);
        if (closingBalance.compareTo(BigDecimal.ZERO) < 0) {
            return OVERDRAFT_FEE_AED;
        }
        return null;
    }

    public BigDecimal calculateDailyInterest(AccountLedgerStore store, int day, int decimalPlaces) {
        BigDecimal closingBalance = store.getClosingBalanceAsOf(day);
        if (closingBalance.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return closingBalance
                .multiply(DAILY_INTEREST_RATE)
                .setScale(decimalPlaces, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateCapitalizedInterest(AccountLedgerStore store, int windowEndDay, int decimalPlaces) {
        BigDecimal total = BigDecimal.ZERO;
        for (int day = 1; day <= windowEndDay; day++) {
            BigDecimal daily = calculateDailyInterest(store, day, decimalPlaces);
            if (daily != null) {
                total = total.add(daily);
            }
        }
        return total;
    }

    public void verifyInterestReconciliation(BigDecimal sumOfDailyAccruals, BigDecimal capitalizedTotal) {
        if (sumOfDailyAccruals.compareTo(capitalizedTotal) != 0) {
            throw new IllegalStateException(
                    "Interest reconciliation failed: sum of daily accruals (" + sumOfDailyAccruals
                            + ") does not equal capitalized total (" + capitalizedTotal + ")"
            );
        }
    }
}