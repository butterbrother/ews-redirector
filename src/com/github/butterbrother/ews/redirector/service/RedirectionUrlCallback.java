package com.github.butterbrother.ews.redirector.service;

import microsoft.exchange.webservices.data.autodiscover.IAutodiscoverRedirectionUrl;
import microsoft.exchange.webservices.data.autodiscover.exception.AutodiscoverLocalException;

/**
 * Отвечает за валидацию EWS URL при автоматическом определении
 */
public class RedirectionUrlCallback implements IAutodiscoverRedirectionUrl {
    @Override
    public boolean autodiscoverRedirectionUrlValidationCallback(String s) throws AutodiscoverLocalException {
        return s.toLowerCase().startsWith("https://");
    }
}
