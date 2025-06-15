package com.example.soundwatch;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RecodeTestData {

    public static List<NoiseLog> getDummyLogs() {
        List<NoiseLog> logs = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        // 2025-04-18
        cal.set(2025, Calendar.APRIL, 18);
        logs.add(createLog(cal, 9, 55, 40, 9, 56, 20, "36.851866, 127.150882", 110, 1));
        logs.add(createLog(cal, 10, 24, 0, 10, 25, 4, "36.836531, 127.150818", 95, 2));
        logs.add(createLog(cal, 18, 56, 54, 18, 59, 0, "36.819573, 127.156790", 108, 3));

        // 2025-04-19
        cal.set(2025, Calendar.APRIL, 19);
        logs.add(createLog(cal, 11, 0, 0, 11, 5, 0, "36.800000, 127.160000", 92, 4));

        return logs;
    }

    private static NoiseLog createLog(Calendar baseCal, int sh, int sm, int ss, int eh, int em, int es, String location, float db, int id) {
        NoiseLog log = new NoiseLog();
        Calendar start = (Calendar) baseCal.clone();
        start.set(Calendar.HOUR_OF_DAY, sh);
        start.set(Calendar.MINUTE, sm);
        start.set(Calendar.SECOND, ss);
        log.setStartTime(new Timestamp(start.getTimeInMillis()));

        Calendar end = (Calendar) baseCal.clone();
        end.set(Calendar.HOUR_OF_DAY, eh);
        end.set(Calendar.MINUTE, em);
        end.set(Calendar.SECOND, es);
        log.setEndTime(new Timestamp(end.getTimeInMillis()));

        log.setLocation(location);
        log.setMaxDb(db);
        log.setId(id);
        return log;
    }
}

