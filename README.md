# In-Memory Account Ledger Core

Staff Software Engineer assessment — in-memory ledger engine with event replay, fee/interest rules, and authorization holds.

## How to run

```bash
./gradlew run
```

This replays the fixed 10-event stream (Day 1-6, accounts ACC-001 AED and ACC-002 BHD) and prints, per day, per account: closing ledger balance and active authorization holds. Rejected events (invalid settlements/reversals/authorizations) print inline as errors at the point they occur during replay.

## How to run tests

```bash
./gradlew test
```

This runs the full test suite: unit tests for the data model, rule engine, and settlement/reversal validation, an integration test replaying the full event stream against several acceptance criteria, and one **deliberately failing test** (`KnownLimitationTest`) documenting a known, accepted gap — see that file's comments, and `AMBIGUITIES.md` #9 / `REJECTED.md` Part B item 8, for why it fails on purpose.

## Project structure

```
src/main/java/com/shyam/ledgercore/
  model/     — Account, Currency, LedgerEvent (sealed) + 5 event records,
               HoldStatus, ActiveHold, AccountLedgerStore
  engine/    — LedgerEngine, HoldManager, SettlementHandler, ReversalHandler
  rules/     — RuleEngine: overdraft fee, interest accrual, authorization approval
  report/    — DailyReportGenerator
  replay/    — EventProcessor, EventStreamFactory (the real E1-E10 data), Main
src/test/java/com/shyam/ledgercore/
  model/, rules/, engine/, replay/ — unit and integration tests
  KnownLimitationTest.java — the required deliberately-failing test
docs/
  ARCHITECTURE.md                — system design, Mermaid diagram
  ACCOUNT_LEDGER_STORE_DESIGN.md — design detail for the core per-account store
  NUMBERS.md                     — every constant chosen, and why
  AMBIGUITIES.md                 — every ambiguity found and how it was resolved
                                    (or explicitly left unresolved, with reasoning)
  REJECTED.md                    — acceptance criteria evaluated, and abandoned approaches
  WORKLOG.md                     — real, timestamped work log
```

## Status

Complete. All acceptance criteria evaluated in `REJECTED.md` (criteria #3, #4, #7, #8 verified correct/incorrect with passing tests; criteria #1, #2, #6 explicitly left unresolved rather than asserted with false confidence — reasoning in `REJECTED.md`). One real bug found and fixed during development, documented in `WORKLOG.md` and `AMBIGUITIES.md` #8.