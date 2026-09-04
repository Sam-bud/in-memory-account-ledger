# AMBIGUITIES.md

Every ambiguity found while building this, and how it was resolved (or explicitly left unresolved, with reasoning, where resolving it would mean inventing behavior the spec doesn't support).

---

## 1. Event processing order vs. value_date order

The 10 events are listed in a specific order, and each has both an `arrivalDay` (when it appears in the stream) and a `valueDate` (the day it's backdated/effective to). These differ for E7 (arrives Day 5, value_date=Day 2) and E9 (arrives Day 6, value_date=Day 2).

**Resolution**: events are processed **in the given list order** (= arrival order), never re-sorted by value_date. `value_date` only affects which days' *balance calculations* include a given event (via `getClosingBalanceAsOf`), not the order events are applied to the store. This distinction matters concretely — see #8 below, where it directly changed a test's correct expected outcome.

## 2. REVERSAL mechanics — exclude vs. offset

The spec doesn't state whether a REVERSAL creates a new offsetting ledger entry (with its own amount) or works by referencing and excluding a prior event's contribution.

**Resolution**: `ReversalEvent` (our data model) carries no amount — only a `referencedEventId`. Balance calculation excludes the referenced event's contribution entirely, gated by the reversal's own `valueDate` (a reversal doesn't retroactively apply to a balance query for a day before the reversal itself takes effect). Chosen because it matches Rule 4 (append-only) most directly — nothing is edited, an event is simply excluded from a specific calculation while remaining permanently in history.

## 3. Does closing-balance calculation need to know about fees/interest specially?

**Resolution**: No. `getClosingBalanceAsOf` is deliberately rule-agnostic — it sums whatever `CreditEvent`/`DebitEvent`/`SettlementEvent` entries exist in the store with valueDate <= day. Once a fee is appended (as a `DebitEvent` with `feeFlag=true`), it's included automatically, like any other event. All rule *decisions* (whether to charge a fee, whether interest applies) live in `RuleEngine`, not in balance calculation.

## 4. Interest eligibility: live-at-the-time balance, or fully-corrected hindsight balance?

Rule 2 says interest applies to "the closing ledger balance, positive balances only" — but doesn't say whether this means the balance as it appeared live on that day, or the balance after all later events (including backdated corrections) are applied.

**Resolution**: `calculateDailyInterest` calls `getClosingBalanceAsOf(day)` at the time it's invoked, which reflects everything appended to the store so far — i.e., hindsight-corrected if called after full replay. We did not build separate "live snapshot vs. corrected" tracking, since the spec provides no mechanism or requirement for preserving a day's live-observed value once later events revise it.

## 5. Interest rounding reconciliation method

Rule 2 requires "rounded daily accruals must sum exactly to the capitalized total," but doesn't specify the reconciliation technique.

**Resolution**: each day's interest is calculated and rounded independently (to the account's currency precision), then summed. `verifyInterestReconciliation` checks the sum against the capitalized total and **throws `IllegalStateException`** if they don't match — it does not silently adjust or force agreement. This was a deliberate choice: the spec's constraint is verified, not engineered around.

## 6. Rounding mode is unspecified

The spec states precision (2dp AED, 3dp BHD) but not which `RoundingMode` to use at exact rounding boundaries.

**Resolution**: `RoundingMode.HALF_UP` used throughout, chosen as the most common default interpretation. Not verified against any alternative the spec might have intended (e.g., HALF_EVEN/banker's rounding).

## 7. E10's three-way BHD split — no specified remainder distribution

10.000 / 3 = 3.333 repeating; at BHD's 3-decimal precision, three exactly equal instalments summing to exactly 10.000 is mathematically impossible. (This directly makes acceptance criterion #7, which claims all three equal 3.334, incorrect: 3 x 3.334 = 10.002, not 10.000.)

**Resolution**: implemented as 3.333 / 3.333 / 3.334 (the third instalment absorbs the remainder). This is **a** valid distribution, not authoritatively **the** correct one — the spec doesn't specify which position should absorb the rounding remainder.

## 8. Authorization approval is evaluated once, in real time — not retroactively revisited

**This ambiguity produced a real, observed test failure during development (see WORKLOG.md Entry 7), not just a theoretical concern.**

Auth-A (E3, Day 2) and its settlement (E5, Day 4) both completed using the ledger balance known at that time (250, then 465 after E4/E5). E7 later arrives (Day 5) backdated to Day 2, retroactively revealing the "true" Day 2 balance was actually negative once known in full. Separately, Auth-B (E8) arrives on Day 5, *after* E7 has already been applied (same day, but E7 precedes E8 in list order) — at that exact moment, ledger balance is genuinely -155.00, so Rule 5 correctly rejects Auth-B.

The spec provides no mechanism to retroactively revisit an authorization decision once later backdated events change the historical picture, and no event type exists for "retroactively invalidate a hold" or "retry a rejected authorization." We do not implement any such mechanism.

**Resolution**: authorization approval is evaluated exactly once, using whatever state exists in the store at the moment the AUTHORIZATION event is processed (in list order). Decisions are never revisited. This was confirmed correct, not assumed — an initial test wrongly expected Auth-B to be approved; investigation showed the rejection was the accurate outcome of Rule 5 applied honestly and sequentially, and the test's expectation was corrected instead of the code.

## 9. Structural gap: an already-completed authorization to settlement lifecycle can be invalidated in hindsight, and the spec provides no rule for it

Auth-A's full lifecycle (hold then settle, E3 then E5) completes using information that a later event (E7) shows, in hindsight, was based on an inaccurate historical balance. By the time this is knowable, real money has already moved via the settlement.

**Resolution: not resolved, deliberately.** No flagging, unwind, or annotation mechanism is built for this. The spec provides no event type or rule for this interaction, and per the assessment's own instruction, we implement only what's asked rather than invent speculative handling for a gap the spec doesn't address.

## 10. "Errors must not leave the account" — no daily-report attachment; printed inline instead

The spec requires errors to appear in per-day output. Our implementation prints rejections (e.g., E6, and E8 in the earlier bug scenario) inline, at the point in replay where they occur — not collected and attached to that day's formal report output.

**Resolution**: this is an implementation choice, not the only valid reading of "prints, per day... errors." An alternative (collecting errors and displaying them grouped by day, alongside balance/fee output) is equally defensible and was not built, per simplicity.

## 11. No retry mechanism for a rejected authorization

If available balance would go negative, Rule 5 rejects the authorization immediately and permanently at that point in the stream. No event type represents "pending/retry."

**Resolution**: rejection is final. Not implemented: any queuing, retry-on-balance-recovery, or pending-authorization state.
