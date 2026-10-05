package com.payguard.automation.diagnostics;

import java.util.ArrayList;
import java.util.List;

public final class ExchangeLog {

    private static final ThreadLocal<List<String>> EXCHANGES = ThreadLocal.withInitial(ArrayList::new);

    private ExchangeLog() {
    }

    public static void record(String exchange) {
        List<String> list = EXCHANGES.get();
        list.add(exchange);
        if (list.size() > 10) {
            list.remove(0);
        }
    }

    public static String renderAndClear() {
        List<String> list = EXCHANGES.get();
        if (list.isEmpty()) {
            EXCHANGES.remove();
            return null;
        }
        String text = String.join("\n---\n", list);
        EXCHANGES.remove();
        return text;
    }

    public static void clear() {
        EXCHANGES.remove();
    }
}
