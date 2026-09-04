package com.shyam.ledgercore.replay;

import com.shyam.ledgercore.engine.HoldManager;
import com.shyam.ledgercore.model.*;
import com.shyam.ledgercore.report.DailyReportGenerator;
import com.shyam.ledgercore.rules.RuleEngine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static final int WINDOW_END_DAY = 6;

    public static void main(String[] args) {
        AccountLedgerStore acc001 = new AccountLedgerStore("ACC-001");
        AccountLedgerStore acc002 = new AccountLedgerStore("ACC-002");

        Map<String, AccountLedgerStore> stores = new LinkedHashMap<>();
        stores.put("ACC-001", acc001);
        stores.put("ACC-002", acc002);

        List<LedgerEvent> eventStream = EventStreamFactory.buildEventStream();

        EventProcessor processor = new EventProcessor();
        processor.replay(eventStream, stores);

        RuleEngine ruleEngine = new RuleEngine();
        HoldManager holdManager = new HoldManager();
        DailyReportGenerator reportGenerator = new DailyReportGenerator(ruleEngine, holdManager);

        for (int day = 1; day <= WINDOW_END_DAY; day++) {
            processor.assessFeesForDay(acc001, day);
            processor.assessFeesForDay(acc002, day);

            reportGenerator.printDailyReport(acc001, day);
            reportGenerator.printDailyReport(acc002, day);
        }
    }
}