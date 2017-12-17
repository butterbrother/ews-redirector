package com.github.butterbrother.ews.redirector.service;

import java.util.logging.Logger;

public class LogNotificator implements Notificator {

    private Logger logger = Logger.getLogger("Notification");

    @Override
    public void error(String caption, String text) {
        logger.severe("[" + caption + "] " + text);
    }
}
