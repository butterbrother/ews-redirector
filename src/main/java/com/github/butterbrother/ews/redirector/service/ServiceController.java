package com.github.butterbrother.ews.redirector.service;

import com.github.butterbrother.ews.redirector.filter.MailFilter;
import com.github.butterbrother.ews.redirector.graphics.TrayControl;

import javax.swing.*;

/**
 * Непосредственно управляет потоками-обработчиками.
 * Устанавливает соединение с EWS
 */
public class ServiceController extends SafeStopService {
    private ExchangeConnector connector = null;
    private PullEventsService pullEvents = null;
    private NewMessagesSearchService newMessages = null;
    private SendService send = null;

    private JButton startStopButton;
    private JButton applyButton;
    private String recipient;
    private boolean deleteRedirected;
    private TrayControl.TrayPopup popup;
    private MailFilter[] filters;

    public ServiceController(
            String domain,
            String email,
            String recipient,
            String login,
            String password,
            String url,
            boolean enableAuto,
            TrayControl.TrayPopup popup,
            JTextField urlField,
            JButton startStopButton,
            JButton applyButton,
            boolean deleteRedirected,
            MailFilter[] filters
    ) {
        this.startStopButton = startStopButton;
        this.applyButton = applyButton;
        this.recipient = recipient;
        this.deleteRedirected = deleteRedirected;
        this.popup = popup;
        this.filters = filters;

        try {
            connector = new ExchangeConnector(domain, login, password, email, url, enableAuto);
            urlField.setText(connector.getCurrentUrl());
            super.runService();
        } catch (Exception startException) {
            popup.error("Connection error", startException.getMessage());
            super.wellDone();
            safeStop();
        }
    }

    /**
     * Запуск обработчиков и ожидание
     */
    @Override
    public void run() {
        if (connector != null) {
            try {
                send = new SendService(connector, deleteRedirected, recipient, popup, filters);
                pullEvents = new PullEventsService(connector, send.getQueue(), popup);
                newMessages = new NewMessagesSearchService(connector, popup, send.getQueue());

                startStopButton.setText("Stop");
                while (super.isActive()) {
                    try {
                        Thread.sleep(200);
                        if (send.isDone() || pullEvents.isDone() || newMessages.isDone())
                            if (super.isActive())
                                safeStop();
                    } catch (InterruptedException ie) {
                        if (super.isActive())
                            safeStop();
                    }
                }
            } catch (Exception e) {
                popup.error("Connection error", e.getMessage());
                if (super.isActive())
                    safeStop();
            }
        }

        super.wellDone();
    }

    /**
     * Плавная остановка с ожиданием завершения работы
     * всех сервисов.
     */
    @Override
    public synchronized void safeStop() {
        super.safeStop();

        startStopButton.setEnabled(false);
        applyButton.setEnabled(false);

        if (pullEvents != null) {
            pullEvents.safeStop();
            while (!pullEvents.isDone()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignore) {
                }
            }
        }

        if (newMessages != null) {
            newMessages.safeStop();
            while (!newMessages.isDone()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignore) {
                }
            }
        }

        if (send != null) {
            send.safeStop();
            while (!send.isDone()) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException ignore) {
                }
            }
        }

        startStopButton.setText("Start");
        startStopButton.setEnabled(true);
        applyButton.setEnabled(true);
    }
}
