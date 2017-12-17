package com.github.butterbrother.ews.redirector.service;

import java.util.Calendar;

public class StderrNotificator implements Notificator {

    @Override
    public void error(String caption, String text) {
        System.err.println(String.format("<%tY.%<tm.%<td %<tH:%<tM:%<tS> [%s] %s>",
                Calendar.getInstance(),
                caption,
                text));
    }
}
