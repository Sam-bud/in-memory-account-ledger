# NUMBERS.md

Every constant chosen in this implementation, and why that value.

## OVERDRAFT_FEE_AED = 25.00
Where used: `RuleEngine.assessOverdraftFee()`
Why this value: Directly specified by the assessment's non-negotiable rules, not derived or estimated. A flat fee, not proportional to the overdraft amount, matching the spec's exact wording ("Overdraft fee: AED 25.00").

## DAILY_INTEREST_RATE = 0.0004 (0.04%)
Where used: `RuleEngine.calculateDailyInterest()`
Why this value: Directly specified ("Daily interest: 0.04% per day"). Expressed as a `BigDecimal` fraction (0.0004) rather than a percentage literal, to multiply directly against balance without a separate division step.

## Currency precision: AED = 2, BHD = 3
Where used: `Currency` enum (`decimalPlaces` field), all rounding operations
Why these values: Directly specified by the spec ("AED is 2 decimal places, BHD is 3"). These reflect each currency's real-world minor unit convention, not an arbitrary choice.

## RoundingMode.HALF_UP
Where used: `RuleEngine.calculateDailyInterest()` (interest rounding)
Why this value: The spec specifies precision but not rounding mode at exact boundary values. HALF_UP was chosen as the most common, least surprising default (rounds 0.5 up rather than to even) rather than an unstated assumption like HALF_EVEN. Documented in AMBIGUITIES.md #6 as an explicit, not verified-correct, choice.

## E10 BHD split: 3.333 / 3.333 / 3.334
Where used: `EventStreamFactory.buildEventStream()`
Why this value, not another valid split: 10.000 / 3 = 3.333 repeating; no three equal BHD amounts (3dp) can sum to exactly 10.000. The remainder (0.001) must go somewhere. We chose to place it on the third instalment rather than the first or split across multiple, purely as a consistent, simple convention (last-absorbs-remainder) — not because the spec indicates this is the intended position. See AMBIGUITIES.md #7.

## feeFlag = false (default) on DebitEvent
Where used: `DebitEvent` record, checked in `RuleEngine.assessOverdraftFee()`
Why this value: All 10 real events from the given stream are genuine transactions, not system-generated fees, so `false` is the correct default. Only fee-generating code (`EventProcessor.assessFeesForDay`) constructs a `DebitEvent` with `feeFlag = true`. Chosen as a real boolean field over an ID-string naming convention (e.g., prefixing fee event IDs with "FEE-") specifically to check actual data rather than pattern-match a string — a naming convention was considered and rejected for this reason.
