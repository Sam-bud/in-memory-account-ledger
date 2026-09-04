package com.shyam.ledgercore;

import com.shyam.ledgercore.engine.HoldManager;
import com.shyam.ledgercore.model.*;
import com.shyam.ledgercore.replay.EventProcessor;
import com.shyam.ledgercore.replay.EventStreamFactory;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DELIBERATELY FAILING TEST — documents a known, accepted limitation.
 *
 * See AMBIGUITIES.md #9: Auth-A's full lifecycle (authorization E3 ->
 * settlement E5) completes using the ledger balance known at that time
 * (250, then 465 after E4/E5). E7 later arrives (Day 5), backdated to
 * Day 2, retroactively showing that Day 2's "true" balance was actually
 * negative once fully known. By the time this is discoverable, Auth-A
 * has already been approved AND already settled — real money already
 * moved.
 *
 * A fully robust ledger system might be expected to detect this kind of
 * retroactive inconsistency: an authorization/settlement that completed
 * using information a later event proves was incomplete. This test
 * asserts that expectation.
 *
 * THIS TEST FAILS BY DESIGN. Per AMBIGUITIES.md #9 and REJECTED.md Part
 * B item #8, we deliberately did NOT build any flagging, unwind, or
 * inconsistency-detection mechanism for this scenario. The spec defines
 * no event type or rule for it, and per the assessment's instruction to
 * build only what's asked rather than invent speculative handling, this
 * gap is left as a known, accepted, and documented limitation rather
 * than solved with unrequested logic.
 *
 * What this failure reveals: the system has no concept of "this
 * completed transaction was approved against information later proven
 * inaccurate." It simply doesn't track or expose that relationship at
 * all. A production system handling real regulatory/financial risk
 * would very likely need this — but building it was out of scope here.
 */
class KnownLimitationTest {

    @Test
    void system_hasNoMechanismToFlag_settlementCompletedAgainstLaterInvalidatedBalance() {
        AccountLedgerStore acc001 = new AccountLedgerStore("ACC-001");
        Map<String, AccountLedgerStore> stores = new LinkedHashMap<>();
        stores.put("ACC-001", acc001);
        stores.put("ACC-002", new AccountLedgerStore("ACC-002"));

        new EventProcessor().replay(EventStreamFactory.buildEventStream(), stores);

        // Auth-A was approved and settled using a Day 2 balance of 250,
        // which E7 later proves was never actually accurate once fully
        // known (true Day 2 balance, with E7 applied, is negative).
        //
        // A more complete system would expose SOME signal that this
        // happened -- e.g. a flag on the settlement, an entry in an
        // "inconsistency log", or similar. This system exposes nothing
        // of the sort. There is no method anywhere in AccountLedgerStore,
        // RuleEngine, SettlementHandler, or the report layer that
        // surfaces this relationship.
        //
        // This assertion intentionally checks for something that does
        // not exist, to make the gap concrete and visible rather than
        // just described in prose.
        boolean systemFlaggedTheInconsistency = false; // no such capability exists

        assertTrue(systemFlaggedTheInconsistency,
                "Known limitation (see AMBIGUITIES.md #9): system does not detect or flag "
                        + "an authorization/settlement lifecycle that later turns out to have been "
                        + "approved against a balance a backdated event proves was inaccurate. "
                        + "Not implemented by design -- see REJECTED.md Part B, item 8.");
    }
}